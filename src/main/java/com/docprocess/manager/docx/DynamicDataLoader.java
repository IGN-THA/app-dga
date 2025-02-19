package com.docprocess.manager.docx;

import com.docprocess.manager.DocumentRenderException;
import jakarta.persistence.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.transform.Transformers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class DynamicDataLoader {



    public static List<HashMap<String, Object>> getData(String tableName, EntityManagerFactory entityManagerFactory, String referenceNumber) throws DocumentRenderException {
        EntityManager entityManager = null;
        try {
            entityManager = entityManagerFactory.createEntityManager();
            String sql = "SELECT * FROM " + tableName + " WHERE id = :referenceNumber";
            Query query = entityManager.createNativeQuery(sql, Tuple.class);
            query.setParameter("referenceNumber", referenceNumber);
            List<Tuple> tuples = query.getResultList();
            List<HashMap<String, Object>> resultList = tuples.stream()
                    .map(tuple -> tuple.getElements().stream()
                            .collect(Collectors.toMap(
                                    TupleElement::getAlias,
                                    element -> tuple.get(element.getAlias()),
                                    (oldValue, newValue) -> newValue,
                                    HashMap::new
                            )))
                    .collect(Collectors.toList());
            return resultList;
        } catch (Exception e) {
            throw new DocumentRenderException("No Data or Column ID is missing or incorrect value on the input; Table Name: " + tableName + "; Ref Number: " + referenceNumber);
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }

    public static List<HashMap<String, Object>> getDataList(String tableName, EntityManagerFactory entityManagerFactory, String condition) throws DocumentRenderException {
        EntityManager entityManager = null;
        try {
            entityManager = entityManagerFactory.createEntityManager();
            String sql = "SELECT * FROM " + tableName + " WHERE " + condition;
            Query query = entityManager.createNativeQuery(sql, Tuple.class);
            List<Tuple> tuples = query.getResultList();
            List<HashMap<String, Object>> resultList = tuples.stream()
                    .map(tuple -> tuple.getElements().stream()
                            .collect(Collectors.toMap(
                                    TupleElement::getAlias,
                                    element -> tuple.get(element.getAlias()),
                                    (oldValue, newValue) -> newValue,
                                    HashMap::new
                            )))
                    .collect(Collectors.toList());
            return resultList;
        } catch (Exception e) {
            throw new DocumentRenderException("No Data or Column ID is missing or incorrect value on the input; Table Name: " + tableName + "; Condition: " + condition);
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }
    }
}
