package org.hps.conditions;


class QueryBuilder {

    static String buildSelect(String tableName, int collectionId, String[] fields, String order) {
        StringBuffer buff = new StringBuffer();
        buff.append("SELECT ");
        if (fields == null) {
            buff.append("* ");
        } else {
            // Always implicitly include the row ID.
            buff.append("id, ");
            for (String fieldName : fields) {
                buff.append(fieldName + ", ");
            }
            buff.delete(buff.length()-2, buff.length()-1);
        }
        buff.append(" FROM " + tableName);
        if (collectionId != -1)
            buff.append(" WHERE collection_id = " + collectionId);
        if (order != null) {
            buff.append(" ORDER BY " + order);
        }
        System.out.println("QueryBuilder.buildSelectQuery: " + buff.toString());
        return buff.toString();
    }
    
    static String buildUpdate(String tableName, int rowId, String[] fields, Object[] values) {
        return null;
    }
    
    static String buildInsert(String tableName, String[] fields, Object[] values) {
        return null;
    }
    
    static String buildDelete(String tableName, int rowId) {
        return null;
    }
    
}
