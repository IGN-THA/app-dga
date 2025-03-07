package com.docprocess.manager.docx;

import com.docprocess.config.ErrorConfig;
import com.docprocess.manager.DocumentRenderException;
import com.docprocess.model.DocumentGenerateQueueData;
import com.docprocess.repository.ConstantParamRepository;
import com.docprocess.repository.ExternalApiInfoRepository;
import com.docprocess.service.QRCodeService;
import com.docprocess.service.impl.QRCodeServiceImpl;
import com.google.gson.JsonObject;
import jakarta.persistence.EntityManagerFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.io.*;
import java.util.*;

import static org.apache.poi.ooxml.POIXMLTypeLoader.DEFAULT_XML_OPTIONS;

@Component
public class RenderDocumentManager {

    ConstantParamRepository constantParamRepositoryObj=null;

    ExternalApiInfoRepository externalApiInfoRepo = null;
    HashMap<String, Object> variables = new HashMap<String, Object>();
    HashMap<String, List<HashMap<String, Object>>> listVariable= new HashMap<String, List<HashMap<String, Object>>>();
    HashMap<String, Boolean> conditionMap = new HashMap<String, Boolean>();

    HashMap<String, JSONObject> extDataMap = new HashMap<>();
    List<HashMap<String, Object>> listMapObject =  new ArrayList<HashMap<String, Object>>();
    Boolean criteriaMatch = true;
    String criteriaEndStr = null;
    EntityManagerFactory endMgrSessionFactory;
    Integer listIndex = 0;
    Boolean rowIterationFlag = false;
    String rowIterationKey = "";
    ArrayList<String> rowCellFieldNameList = new ArrayList<String>();
    ArrayList<Integer> dynamicCellIndex = new ArrayList<Integer>();
    ArrayList<XWPFRun> xpfRunList = new ArrayList<XWPFRun>();
    Integer newImageCounter = 0;
    String folderPathForImage  = null;

    private QRCodeService qrCodeService;

    static Logger logger = LogManager.getLogger(RenderDocumentManager.class);
    public boolean renderDocFromTemplate(String renderedFilePath, String folderPath, String templatePath, String tableName, EntityManagerFactory sessionFactory, DocumentGenerateQueueData docData, ConstantParamRepository constantParamRepository, ExternalApiInfoRepository externalApiInfoRepository) throws Exception {

        InputStream templateInputStream=null;
        FileOutputStream fout=null;
        XWPFDocument doc=null;
        try {
            this.folderPathForImage = folderPath;
            if(!new File(templatePath).exists()) return false;
            templateInputStream = new FileInputStream(templatePath);
            endMgrSessionFactory = sessionFactory;
            String referenceNumberForData = (docData.getQueryRecordId()!=null && !docData.getQueryRecordId().isEmpty())? docData.getQueryRecordId(): docData.getReferenceNumber();
            List qList = DynamicDataLoader.getData(tableName, sessionFactory, referenceNumberForData);

            if(qList!=null && !qList.isEmpty())
                variables = (HashMap<String, Object>) qList.get(0);
            else throw new DocumentRenderException("Not records found on the view "+tableName);

            constantParamRepositoryObj = constantParamRepository;
            externalApiInfoRepo = externalApiInfoRepository;

            doc = new XWPFDocument(templateInputStream);
            Iterator<IBodyElement> iterator = doc.getBodyElementsIterator();
            Integer rootElement = 0;
            ArrayList<Integer> removeRootElementList = new ArrayList<Integer>();
            ArrayList<Integer> removeChildElementList = new ArrayList<Integer>();

            while (iterator.hasNext()) {
                IBodyElement ibody = iterator.next();
                replaceDocumentElement(ibody, rootElement, rootElement, removeChildElementList, removeRootElementList);
                rootElement++;
            }
            int mainElement = 0;
            for (Integer remove : removeChildElementList) {
                doc.removeBodyElement(remove - mainElement);
                mainElement++;
            }


            fout = new FileOutputStream(new File(renderedFilePath + "/" + docData.getSfid() + "_" + docData.getDocumentName() + ".docx"));
            doc.write(fout);

        }catch(Exception e){

            String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "renderDocFromTemplate", e);
            throw new DocumentRenderException(errorMessage);
        }finally {
            if(doc!=null)doc.close();
            if(fout!=null)fout.close();
            if(templateInputStream!=null)templateInputStream.close();
        }

        if(newImageCounter>1){
            newImageCounter=0;
            renderDocFromTemplate(renderedFilePath, folderPathForImage,renderedFilePath + "/" + docData.getSfid() + "_" + docData.getDocumentName() + ".docx", tableName, sessionFactory,  docData,  constantParamRepository, externalApiInfoRepository);
        }


        return true;
    }

    private void replaceDocumentElement(IBodyElement ibody, Integer childElement, Integer rootElement, ArrayList<Integer> removeElementList, ArrayList<Integer> removeRootElementList) throws DocumentRenderException {

        if (ibody instanceof XWPFParagraph) {
            replaceElement(((XWPFParagraph) ibody), childElement, rootElement, removeElementList, removeRootElementList);
        } else if(ibody instanceof XWPFTable){
            Integer rawIndex=0;
            ArrayList<Integer> innerRemoveRowElementList = new ArrayList<Integer>();
            for(XWPFTableRow tableRow : ((XWPFTable) ibody).getRows()){
                Integer cellIndex=0;
                ArrayList<Integer> innerRemoveRootElementList = new ArrayList<Integer>();
                for(XWPFTableCell tableCell :  tableRow.getTableCells()){
                    Integer innnerRootElement = 0;
                    ArrayList<Integer> innerRemoveElementList = new ArrayList<Integer>();
                    for(IBodyElement innerIbody : tableCell.getBodyElements()){
                        replaceDocumentElement(innerIbody, innnerRootElement, cellIndex, innerRemoveElementList, innerRemoveRootElementList);
                        if (innerIbody instanceof XWPFParagraph)   innnerRootElement++;
                    }
                    int mainElement=0;
                    Integer bodyElementSize = tableCell.getBodyElements().size();
                    Integer paraSize = tableCell.getParagraphs().size();
                    Boolean onlyParaElement = (bodyElementSize==paraSize);
                    for(Integer remove : innerRemoveElementList){
                        tableCell.removeParagraph(remove-mainElement);
                        mainElement++;
                    }
                    if(((onlyParaElement && tableCell.getParagraphs().size()==0) || tableCell.getBodyElements().size()==0) && !innerRemoveRootElementList.contains(cellIndex)) innerRemoveRootElementList.add(cellIndex);
                    cellIndex++;
                }
                int counter=0;
                for(Integer cellIndx : innerRemoveRootElementList) {
                    tableRow.removeCell(cellIndx-counter);
                    counter++;
                }
                if(tableRow.getTableCells().size()==0)  innerRemoveRowElementList.add(rawIndex);
                rawIndex++;
            }
            if(rowIterationFlag){
                Boolean skipFirstRecord = true;
                for(HashMap<String, Object> listMap : listVariable.get(rowIterationKey)){
                    if(skipFirstRecord) {
                        skipFirstRecord = false;
                        continue;
                    }else{
                        int copyingRow = ((XWPFTable) ibody).getRows().size()-1;
                        XWPFTableRow row = ((XWPFTable) ibody).createRow();
                        Integer dynamicValueCell=0;
                        for (int cellIndx = 0; cellIndx < row.getTableCells().size(); cellIndx++) {
                            XWPFTableCell orgCell = ((XWPFTable) ibody).getRow(copyingRow).getCell(cellIndx);
                            XWPFTableCell tabCell = row.getTableCells().get(cellIndx);
                            tabCell.getCTTc().setTcPr(orgCell.getCTTc().getTcPr());
                            if(dynamicCellIndex.contains(cellIndx)) {
                                tabCell.setColor(orgCell.getColor());
                                String key = rowCellFieldNameList.get(dynamicValueCell);
                                XWPFRun xwpfRun = xpfRunList.get(dynamicValueCell);
                                String dynaCellValStr = "";
                                if(key.indexOf("ROW_NUM")!=-1) {
                                    dynaCellValStr = String.valueOf(listIndex+2);
                                }else if(key.indexOf("FORMAT_")!=-1) {
                                    String val[] = TemplateFormatter.formatValue(key, listMap);
                                    dynaCellValStr = val[0];
                                }else {
                                    Object dynaCellVal = listMap.get(key.substring(key.indexOf('.') + 1));
                                    dynaCellValStr = (dynaCellVal != null) ? dynaCellVal.toString() : "";
                                }
                                XWPFParagraph xwpfParag = (tabCell.getParagraphs().size() == 0) ? tabCell.addParagraph() : tabCell.getParagraphs().get(0);
                                xwpfParag.setAlignment(orgCell.getParagraphs().get(0).getAlignment());
                                XWPFRun pRun = xwpfParag.createRun();
                                pRun.setColor(xwpfRun.getColor());
                                pRun.setFontFamily(xwpfRun.getFontFamily());
                                pRun.setFontSize(xwpfRun.getFontSize());
                                pRun.setText(dynaCellValStr);
                                dynamicValueCell++;
                            }else {
                                copyCellDetailsFromOriginal(orgCell, tabCell);
                            }
                        }
                        listIndex++;
                    }
                }
                xpfRunList.clear();
                rowCellFieldNameList.clear();
                dynamicCellIndex.clear();
                rowIterationFlag=false;
                listIndex=0;
            }
            int counter=0;
            for(Integer rawIndx : innerRemoveRowElementList) {
                ((XWPFTable) ibody).removeRow(rawIndx-counter);
                counter++;
            }

        }
    }

    private void copyCellDetailsFromOriginal(XWPFTableCell orgCell, XWPFTableCell tabCell){
        Boolean firstParagraph = true;
        XWPFParagraph newParagraph=null;
        for(XWPFParagraph paragraph : orgCell.getParagraphs()){
            if(firstParagraph)
                newParagraph = (tabCell.getParagraphs().size() == 0) ? tabCell.addParagraph() : tabCell.getParagraphs().get(0);
            else newParagraph = tabCell.addParagraph();

            for(XWPFRun xwpfRun : paragraph.getRuns()){
                XWPFRun pRun = newParagraph.createRun();
                pRun.setColor(xwpfRun.getColor());
                pRun.setFontFamily(xwpfRun.getFontFamily());
                pRun.setFontSize(xwpfRun.getFontSize());
                pRun.setText(xwpfRun.getText(0));
            }
        }

    }

    private void replaceElement(XWPFParagraph paragraph, Integer childElement, Integer rootElement, ArrayList<Integer> removeElementList, ArrayList<Integer> removeRootElementList) throws DocumentRenderException {
        ArrayList<Integer> removeParamElementList = new ArrayList<Integer>();
        HashMap<Integer, String> replaceElementMap = new HashMap<Integer, String>();
        Integer intXPRun = 0;
        Boolean removeElement = false;
        String paraText = paragraph.getText();
        String indx = (criteriaEndStr!=null && !criteriaMatch)?criteriaEndStr:"{!";
        int startKeyPara = paraText.indexOf(indx, 0);
        if(startKeyPara==-1){
            if(!criteriaMatch){
                removeElementList.add(childElement);
            }
            return;
        }
        int endKeyPara = paraText.indexOf(125, startKeyPara);
        int textPos = 0;
        String paraKey = paraText.substring(startKeyPara, endKeyPara+1);
        String searchText = "";
        Boolean flagImage=false;
        //String barCodeKeyName="";
        Integer frmIndx = 0;
        for (XWPFRun prun : paragraph.getRuns()) {

            String st = prun.getText(0);
            if(st==null) {
                intXPRun++;
                continue;
            }
            Integer prunLength = st.length();
            textPos = textPos + prunLength;
            if (textPos > startKeyPara)
                searchText = searchText + st;
            if (st != null && (st.contains(paraKey) || searchText.contains(paraKey))) {
                try {
                    st = searchText;
                    int startKey = st.indexOf("{!", 0);





                    while (startKey != -1) {
                        int keyEnd = st.indexOf(125, startKey);
                        if (keyEnd == -1) {
                            searchText = st.substring(startKey);
                            st = st.substring(0, startKey);

                            startKeyPara = getLatestIndexOfTag(paraText, frmIndx);

                            if (startKeyPara != -1) {
                                //startKeyPara = paraText.indexOf("{!");
                                endKeyPara = startKeyPara;
                                //paraKey = paraText.substring(startKeyPara-1, endKeyPara + 1);
                            }


                            break;
                        }
                        String key = st.substring(startKey + 2, keyEnd);

                        if (key.startsWith("BEGIN")) {
                            Object[] condElement = TemplateEngine.validateCondition(key, variables);
                            String beginIndexStr = (String) condElement[1];
                            String endIndexStr = "{!" + beginIndexStr.replace("BEGIN", "END") + "}";
                            int endCondIndex = st.indexOf(endIndexStr, startKey);
                            if (endCondIndex == -1) {
                                conditionMap.put(endIndexStr.substring(2, endIndexStr.length() - 1), (Boolean) condElement[0]);
                                criteriaMatch = (Boolean) condElement[0];
                                if (criteriaMatch) st = st.replace("{!" + key + "}", "");
                                else st = st.substring(0, startKey);
                                criteriaEndStr = endIndexStr;
                            } else {
                                if ((Boolean) condElement[0]) {
                                    st = st.replace("{!" + key + "}", "");
                                    st = st.replace(endIndexStr, "");
                                } else {
                                    String substr = st.substring(startKey, endCondIndex + 6);
                                    st = st.replace(substr, "");
                                }
                                endKeyPara = endCondIndex + 5;
                            }
                        } else if (key.startsWith("END")) {
                            conditionMap.remove(key);
                            criteriaMatch = true;
                            criteriaEndStr = null;
                            st = st.replace("{!" + key + "}", "");
                        } else if (key.startsWith("CONST")) {
                            String keyName = key.substring(key.indexOf("(") + 1, key.length() - 1);
                            String val = constantParamRepositoryObj.findByConstKey(keyName).getConstValue();
                            if (val != null) st = st.replace("{!" + key + "}", val);
                        } else if (key.startsWith("BARCODE")) {
                            if(newImageCounter<1) {
                                flagImage = generateBarCode(prun, key, variables);
                                //barCodeKeyName = key;
                                if (flagImage) {
                                    st = st.replace("{!" + key + "}", "");
                                    newImageCounter++;
                                }
                            }else{
                                flagImage=true;
                                newImageCounter++;
                                return;
                            }
                        }else if (key.startsWith("QRCODE")) {
                            if(newImageCounter<1) {
                                flagImage = generateQRCode(prun, key, variables);

                                if (flagImage) {
                                    st = st.replace("{!" + key + "}", "");
                                    newImageCounter++;
                                }
                            }else{
                                flagImage=true;
                                newImageCounter++;
                                return;
                            }
                        } else if (key.startsWith("FORMAT")) {
                            String val[] = TemplateFormatter.formatValue(key, variables);
                            if(val[1]!=""){
                                rowIterationKey = val[1];
                                listMapObject = listVariable.get(rowIterationKey);
                                if (listMapObject.size() > 0) {
                                    String newKey = key.replace(val[1]+".","");
                                    String valTemp[] = TemplateFormatter.formatValue(newKey, listMapObject.get(listIndex));
                                    String dynaCellValStr = valTemp[0];
                                    st = st.replace("{!" + key + "}", dynaCellValStr);
                                    updateRowInfoFromListIndex(newKey, prun, rootElement);
                                } else {
                                    st = st.replace("{!" + key + "}", "");
                                }
                            }else{
                                st = st.replace("{!" + key + "}", val[0]);
                            }
                        } else if (key.startsWith("LIST")) {
                            HashMap listMap = TemplateEngine.getListDetailsFromKey(key, variables);
                            listVariable.put(listMap.get("keyName").toString(), (DynamicDataLoader.getDataList(listMap.get("tableName").toString(), endMgrSessionFactory, listMap.get("whereClause").toString())));
                            st = st.replace("{!" + key + "}", "");
                            rowCellFieldNameList.clear();
                            xpfRunList.clear();
                        } else if (key.startsWith("IMAGE")) {
                            //String keyVal = key.substring(key.indexOf("(") + 1, key.length() - 1);
                            //String val = (variables.get(keyVal) != null) ? variables.get(keyVal).toString() : "Scenario_1";
                            flagImage = printImage(prun, key, variables);
                            if (flagImage) {
                                st = st.replace("{!" + key + "}", "");
                                newImageCounter++;
                            }else{
                                st = st.replace("{!" + key + "}", "");
                            }
                        } else if (key.startsWith("ROW_NUM")) {
                            st = st.replace("{!ROW_NUM()}", String.valueOf(listIndex + 1));
                            updateRowInfoFromListIndex(key, prun, rootElement);
                        } else if (key.startsWith("EXTDATA")){
                            String[] keys = key.split("[()]");
                            String keyString = keys[1].replace("'", "").replace("\"", "");
                            String[] keyArr = keyString.split(",");
                            JSONObject responseObj = ExternalApiInfoManager.extData(keyArr, variables,extDataMap, externalApiInfoRepo);
                            if (responseObj != null){
                                String dynaValue = responseObj.getString(keyArr[2]);
                                st = st.replace("{!" + key + "}", dynaValue);
                            }else {
                                st = st.replace("{!" + key + "}", "");
                            }
                        } else {
                            if (key.indexOf(".") > 0) {
                                rowIterationKey = key.substring(0, key.indexOf('.'));
                                listMapObject = listVariable.get(rowIterationKey);
                                if (listMapObject.size() > 0) {
                                    Object dynaCellVal = listMapObject.get(listIndex).get(key.substring(key.indexOf('.') + 1));
                                    String dynaCellValStr = (dynaCellVal != null) ? dynaCellVal.toString() : "";
                                    st = st.replace("{!" + key + "}", dynaCellValStr);
                                    updateRowInfoFromListIndex(key, prun, rootElement);
                                } else {
                                    st = st.replace("{!" + key + "}", "");
                                }

                            } else {
                                String val = (variables.get(key) != null) ? variables.get(key).toString() : "";
                                st = st.replace("{!" + key + "}", val);
                            }
                        }
                        startKey = st.indexOf("{");
                        //if(startKey==-1 && nextIndx!=-1 && textPos > nextIndx){
                        //    searchText = st.substring(st.length()-(textPos-nextIndx));
                         //   st = st.substring(0,st.length()-(textPos-nextIndx));
                        //}else {
                        //    frmIndx = nextIndx;
                        //    nextIndx= paraText.indexOf("{!", frmIndx);
                            searchText = "";
                        //}
                        frmIndx ++;
                    }
                    if (st.trim().length() == 0 && !flagImage && !xpfRunList.contains(prun)) {
                        removeParamElementList.add(intXPRun);
                        removeElement = true;
                    } else {
                        replaceElementMap.put(intXPRun, st);
                    }
                    indx = (criteriaEndStr != null && !criteriaMatch) ? criteriaEndStr : "{!";
                    startKeyPara = paraText.indexOf(indx, endKeyPara);
                    if (startKeyPara != -1) {
                        endKeyPara = paraText.indexOf(125, startKeyPara);
                        paraKey = paraText.substring(startKeyPara, endKeyPara + 1);
                    }
                }catch(DocumentRenderException e){
                    throw e;
                }catch (Exception e){

                    String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "replaceElement", e);
                    throw new DocumentRenderException(errorMessage);
                }
            } else {
                if ((startKeyPara!=-1 && textPos > startKeyPara) || !criteriaMatch) {
                    removeParamElementList.add(intXPRun);
                    removeElement = true;
                }
            }

            intXPRun++;
        }
        replaceElement(replaceElementMap, paragraph);
        removeParamElementList.removeAll(replaceElementMap.keySet());

        if(removeParamElementList.size()>0){
            removeElement(removeParamElementList, paragraph);
        }

        //if(flagImage){
//            XWPFRun run = paragraph.createRun();
//            generateBarCode(run, barCodeKeyName, variables);
//        }

        Boolean removeRootObject = true;
        if(paragraph.getBody() instanceof XWPFTableCell) {
            removeRootObject = (((XWPFTableCell) paragraph.getBody()).getTableRow().getTableCells().size() == 1);
        }
        if(paragraph.getRuns().size()==0 && removeElement && removeRootObject){
            removeElementList.add(childElement);
        }
    }

    public Integer getLatestIndexOfTag(String paraTxtm, Integer frmIndx){
        Integer currIndx = 0;
        while (frmIndx>0){
            currIndx = paraTxtm.indexOf("{!",currIndx);
            frmIndx--;
            currIndx++;
        }
        return currIndx;
    }

    public void updateRowInfoFromListIndex(String key, XWPFRun prun, Integer rootElement){
        if (listMapObject.size() > 0) {

            if (listMapObject.size() > 1) {
                rowIterationFlag = true;
                xpfRunList.add(prun);
                rowCellFieldNameList.add(key);
                dynamicCellIndex.add(rootElement);
            }
        }
    }

    private void replaceElement(HashMap<Integer, String> replaceElementMap, XWPFParagraph paragraph){
        for(Integer intPos : replaceElementMap.keySet()){
            paragraph.getRuns().get(intPos).setText(replaceElementMap.get(intPos),0);
        }
    }

    private void removeElement(ArrayList<Integer> removeElementList, XWPFParagraph paragraph){
        int mainElement=0;
        for(Integer removeInt : removeElementList){

            //XWPFRun xwpFRun = paragraph.getRuns().get(removeInt-mainElement);
            //if (xwpFRun.getText(0).equals(xwpFRun.toString()))
            paragraph.removeRun(removeInt-mainElement);

            mainElement++;


        }
    }
    private Boolean printImage(XWPFRun prun, String key, Map<String, ?> mappings) {
        Object[] element = new Object[2];
        int startKey = key.indexOf('(');
        if (startKey == -1)
            return null;
        else {
            String beginVal = key.substring(0, startKey);
            int keyEnd = key.indexOf(41, startKey);
            String imageParam = key.substring(startKey + 1, keyEnd);
            String[] imageParams = imageParam.split(",");
            if (imageParams.length == 3) {
                InputStream inStream = null;
                String imgName = mappings.get(imageParams[0])!=null?mappings.get(imageParams[0]).toString():"";
                try {
                    inStream = new FileInputStream(new File(folderPathForImage + "/Image/" + imgName + ".png"));
                } catch (FileNotFoundException e) {
                    String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "printImage", e);
                    logger.error(errorMessage);
                }
                if (inStream != null) {
                    try {
                        XWPFPicture xwpfPic = prun.addPicture(inStream, XWPFDocument.PICTURE_TYPE_JPEG, "ImageFile", Units.toEMU(Integer.parseInt(imageParams[1])), Units.toEMU(Integer.parseInt(imageParams[2])));
                        inStream.close();
                        return true;
                    } catch (InvalidFormatException e) {
                        String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "printImage", e);
                        logger.error(errorMessage);
                    } catch (IOException e) {
                        String errorMessage = ErrorConfig.getErrorMessages(this.getClass().getName(), "printImage", e);
                        logger.error(errorMessage);
                    }
                }
            }
        }
        return false;
    }

    private static Boolean generateBarCode(XWPFRun prun, String key, Map<String, ?> mappings) {
        Object[] element = new Object[2];
        int startKey = key.indexOf('(');
        if (startKey == -1)
            return null;
        else {
            String beginVal = key.substring(0, startKey);
            int keyEnd = key.indexOf(41, startKey);
            String barCodeParam = key.substring(startKey + 1, keyEnd);
            String[] barcodeParamArray =  barCodeParam.split(",");
            if(barcodeParamArray.length==3) {
                GenerateBarCode barcodeObj = new GenerateBarCode();
                InputStream inStream = barcodeObj.generateBarCode(mappings.get(barcodeParamArray[0]).toString(), Integer.parseInt(barcodeParamArray[1]), Integer.parseInt(barcodeParamArray[2]));
                if(inStream!=null) {
                    try {
                        XWPFPicture xwpfPic = prun.addPicture(inStream, XWPFDocument.PICTURE_TYPE_JPEG, "ImageFile", Units.toEMU(Integer.parseInt(barcodeParamArray[1])-80), Units.toEMU(Integer.parseInt(barcodeParamArray[2])-35));
                        inStream.close();
                        return true;
                    } catch (InvalidFormatException e) {
                        String errorMessage = ErrorConfig.getErrorMessages(RenderDocumentManager.class.getName(), "generateBarCode", e);
                        logger.error(errorMessage);
                    } catch (IOException e) {
                        String errorMessage = ErrorConfig.getErrorMessages(RenderDocumentManager.class.getName(), "generateBarCode", e);
                        logger.error(errorMessage);
                    }
                }
            }
        }
        return false;
    }

    private Boolean generateQRCode(XWPFRun prun, String key, Map<String, ?> mappings) {
        int startKey = key.indexOf('(');
        if (startKey == -1)
            return null;
        else {
            int keyEnd = key.indexOf(41, startKey);
            String qrCodeParam = key.substring(startKey + 1, keyEnd);
            String[] qrCodeParamArray =  qrCodeParam.split(",");
            if(qrCodeParamArray.length==3) {
                qrCodeService = new QRCodeServiceImpl();
                InputStream inStream = qrCodeService.generateQRCode(mappings.get(qrCodeParamArray[0]).toString(), Integer.parseInt(qrCodeParamArray[1]), Integer.parseInt(qrCodeParamArray[2]));
                if(inStream!=null) {
                    try {
                        prun.addPicture(inStream, XWPFDocument.PICTURE_TYPE_PNG, "ImageFile", Units.toEMU(Integer.parseInt(qrCodeParamArray[1])-80), Units.toEMU(Integer.parseInt(qrCodeParamArray[2])-35));
                        inStream.close();
                        return true;
                    } catch (InvalidFormatException e) {
                        String errorMessage = ErrorConfig.getErrorMessages(RenderDocumentManager.class.getName(), "generateQRCode", e);
                        logger.error(errorMessage);
                    } catch (IOException e) {
                        String errorMessage = ErrorConfig.getErrorMessages(RenderDocumentManager.class.getName(), "generateQRCode", e);
                        logger.error(errorMessage);
                    }
                }
            }
        }
        return false;
    }

}
