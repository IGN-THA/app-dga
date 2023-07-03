package com.docprocess.manager.docx;

import com.docprocess.manager.DocumentRenderException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;


public class DynamicDataLoader {



    public static List getData(String tableName, EntityManagerFactory sessionFactory, String referenceNumber) throws DocumentRenderException{
        try {
            Session session = sessionFactory.unwrap(SessionFactory.class).openSession();
            Query q = session.createSQLQuery("select * from " + tableName + " where id='" + referenceNumber + "'");
            q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
            List qList = q.list();
            session.close();
            return qList;
        }catch(Exception e){
            throw new DocumentRenderException("No Data or Column ID is missing or incorrect value on the input; Table Name: "+tableName+"; Ref Number:"+referenceNumber);
        }
    }

    public static List getDataList(String tableName, EntityManagerFactory sessionFactory, String condition) throws DocumentRenderException{
        try {
            Session session = sessionFactory.unwrap(SessionFactory.class).openSession();
            Query q = session.createSQLQuery("select * from " + tableName + " where " + condition);
            q.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
            List qList = q.list();
            session.close();
            return qList;
        } catch (Exception e) {
            throw new DocumentRenderException("No Data or Column ID is missing or incorrect value on the input; Table Name: " + tableName + "; Condition:" + condition);
        }
    }
}
