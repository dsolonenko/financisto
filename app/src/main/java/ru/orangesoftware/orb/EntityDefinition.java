/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.orb;

import javax.persistence.PersistenceException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

class EntityDefinition {
	
	static class Builder {
		private final Class<?> clazz;
		private Constructor<?> constructor;
		private String tableName; 
		private FieldInfo idField;
		private final List<FieldInfo> fields = new LinkedList<>();
		
		Builder(Class<?> clazz) {
			this.clazz = clazz;
		}
		
		public void withConstructor(Constructor<?> constructor) {
			this.constructor = constructor;
		}
		
		Builder withTable(String tableName) {
			this.tableName = tableName;
			return this;
		}
		
		Builder withIdField(FieldInfo fi) {
			idField = fi;
			fields.add(fi);
			return this;
		}
		
		Builder withField(FieldInfo fi) {
			fields.add(fi);
			return this;
		}
		
		EntityDefinition create() {
			if (tableName == null) {
				tableName = clazz.getSimpleName().toUpperCase();
			}
			return new EntityDefinition(constructor, tableName, idField, fields.toArray(new FieldInfo[fields.size()]));
		}

	}

	final Constructor<?> constructor;
	final String tableName;
	final FieldInfo idField;
	final FieldInfo[] fields;
//	final String[] primitiveColumns;
//	final JoinEntity[] joinEntities;
	final String sqlQuery;
	final HashMap<String, FieldInfo> fieldToInfoMap = new HashMap<>();
	
	private EntityDefinition(Constructor<?> constructor, String tableName, FieldInfo idField, FieldInfo[] fields) {
		this.constructor = constructor;
		this.tableName = tableName;
		this.idField = idField;
		this.fields = fields;
		this.sqlQuery = prepareSqlQuery();
		prepareColumns();
//		this.primitiveColumns = prepareColumns();
//		this.joinEntities = prepareJoinEntities();
	}

	private void prepareColumns() {
		FieldInfo[] fields = this.fields;
		for (FieldInfo f : fields) {
			fieldToInfoMap.put(f.field.getName(), f);
		}
	}

//	private JoinEntity[] prepareJoinEntities() {
//		LinkedList<JoinEntity> entities = new LinkedList<JoinEntity>();
//		prepareJoinEntities(entities, this, 0, true);
//		return entities.toArray(new JoinEntity[entities.size()]);
//	}
//	
//	private int prepareJoinEntities(LinkedList<JoinEntity> entities, EntityDefinition ed, int parentIndex, boolean required) {
//		int index = parentIndex;
//		FieldInfo[] fields = ed.fields;
//		int count = fields.length;
//		for (int i=0; i<count; i++) {
//			FieldInfo f = fields[i];
//			if (!f.type.isPrimitive()) {
//				boolean isRequired = required && f.required; 
//				EntityDefinition eed = EntityManager.getEntityDefinitionOrThrow(f.field.getType());
//				JoinEntity e = new JoinEntity(f, eed, ++index, parentIndex, isRequired);
//				entities.add(e);
//				index = prepareJoinEntities(entities, eed, index, isRequired);
//			}
//		}		
//		return index;
//	}

	public long getId(Object entity) {
		try {
			return idField.field.getLong(entity);
		} catch (Exception e) {
			throw new PersistenceException("Unable to get id from "+entity, e); 
		}
	}

    public void setId(Object entity, long id) {
        try {
            idField.field.setLong(entity, id);
        } catch (Exception e) {
            throw new PersistenceException("Unable to set id for "+entity, e);
        }
    }

	public String getColumnForField(String field) {
		if (field.indexOf('.') > 0) {
			String[] path = field.split("\\.");
			StringBuilder e = new StringBuilder("e");
			int count = path.length;
			EntityDefinition ed = this;
			for (int i=0; i<count; i++) {
				String f = path[i];
				FieldInfo fi = ed.getFieldInfo(f);				
				if (fi.type.isPrimitive()) {
					e.append("_").append(fi.columnName);
					break;
				} else {
					e.append(fi.index);
					ed = EntityManager.getEntityDefinitionOrThrow(fi.field.getType());
				}
			}
			return e.toString();
		} else {
			FieldInfo fi = getFieldInfo(field);
			return "e_"+fi.columnName;			
		}
	}
	
	private FieldInfo getFieldInfo(String field) {
		FieldInfo f = fieldToInfoMap.get(field);
		if (f == null) {
			throw new IllegalArgumentException("Unknown field ["+field+"] for "+constructor.getDeclaringClass());
		}
		return f;
	}

	protected String prepareSqlQuery() {
		StringBuilder sb1 = new StringBuilder("select ");
		sb1.append("e").append(".").append(idField.columnName).append(" as ").append(EntityManager.DEF_ID_COL);
		StringBuilder sb2 = new StringBuilder();
		sb2.append(" from ").append(tableName).append(" as e");
		prepareSqlQuery(this, sb1, sb2, "e", true);		
		return sb1.append(sb2).toString();		
	}
	
	protected void prepareSqlQuery(EntityDefinition ed, StringBuilder sbColumns, StringBuilder sbJoins, String pe, boolean required) {
		FieldInfo[] fields = ed.fields;
		for (FieldInfo f : fields) {
			if (f.type.isPrimitive()) {
				appendColumn(sbColumns, pe, f.columnName);
			} else {
				String e = pe+f.index;
				boolean isRequired = required & f.required;
				EntityDefinition edJoin = EntityManager.getEntityDefinitionOrThrow(f.field.getType());				
				sbJoins.append(isRequired ? " inner join " : " left outer join ").append(edJoin.tableName).append(" as ").append(e);
				sbJoins.append(" on ").append(e).append(".").append(ed.idField.columnName).append("=").append(pe).append(".").append(f.columnName);
				prepareSqlQuery(edJoin, sbColumns, sbJoins, e, isRequired);
			}
		}
	}

//	protected String prepareSqlQuery() {
//		String e0 = "e0";
//		StringBuilder sb1 = new StringBuilder("select ");
//		sb1.append(e0).append(".").append(idField.columnName).append(" as _id");
//		StringBuilder sb2 = new StringBuilder();
//		appendColumns(sb1, e0, primitiveColumns);
//		JoinEntity[] entities = this.joinEntities;
//		for (JoinEntity entity : entities) {
//			String e = "e"+entity.index;
//			String pe = "e"+entity.parentIndex;
//			FieldInfo f = entity.field;
//			EntityDefinition ed = entity.entity;
//			sb2.append(entity.required ? " inner join " : " outer join ").append(ed.tableName).append(" as ").append(e);
//			sb2.append(" on ").append(e).append(".").append(ed.idField.columnName).append("=").append(pe).append(".").append(f.columnName);
//			appendColumns(sb1, e, ed.primitiveColumns);
//		}
//		sb1.append(" from ").append(tableName).append(" as ").append(e0).append(" ").append(sb2.toString());
//		return sb1.toString();
//	}
//
//	private void appendColumns(StringBuilder sb, String e, String[] columns) {
//		for (String c : columns) {
//			appendColumn(sb, e, c);
//		}
//	}

	private void appendColumn(StringBuilder sb, String e, String c) {
		sb.append(", ").append(e).append(".").append(c).append(" as ").append(e).append("_").append(c);
	}
}
