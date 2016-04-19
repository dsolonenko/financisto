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
package ru.orangesoftware.financisto.model;

import java.util.*;

import android.database.Cursor;

public class CategoryTree<T extends CategoryEntity<T>> implements Iterable<T> {
	
	private final ArrayList<T> roots;

	public CategoryTree(ArrayList<T> roots) {
		this.roots = roots;
	}
	
	public CategoryTree() {
		this.roots = new ArrayList<T>();
	}
	
	public static <T extends CategoryEntity<T>> CategoryTree<T> createFromCursor(Cursor c, NodeCreator<T> creator) {
		ArrayList<T> roots = new ArrayList<T>();
		T parent = null;
		while (c.moveToNext()) {
			T category = creator.createNode(c);
			while (parent != null) {
				if (category.left > parent.left && category.right < parent.right) {
					parent.addChild(category);
					break;
				} else {
					parent = parent.parent;
				}										
			}
			if (parent == null) {
				roots.add(category);
			}
			if (category.id > 0 && (category.right - category.left > 1)) {
				parent = category;
			}
		}	
		return new CategoryTree<T>(roots);
	}

    public void insertAtTop(T category) {
        roots.add(0, category);
    }

    public static interface NodeCreator<T> {

        T createNode(Cursor c);
    }
	public Map<Long, T> asMap() {
		Map<Long, T> map = new HashMap<Long, T>();
		initializeMap(map, this);
		return map;
	}

	private void initializeMap(Map<Long, T> map, CategoryTree<T> tree) {
		for (T c : tree) {
			map.put(c.id, c);
			if (c.hasChildren()) {
				initializeMap(map, c.children);
			}
		}
	}

	@Override
	public Iterator<T> iterator() {
		return roots.iterator();
	}

	public boolean isEmpty() {
		return roots.isEmpty();
	}

	public void add(T child) {
		roots.add(child);
	}

    public void remove(T category) {
        roots.remove(category);
    }

	public int indexOf(T child) {
		return roots.indexOf(child);
	}
	
	public int size() {
		return roots.size();
	}
	
	public T getAt(int pos) {
		return roots.get(pos);
	}
    
    public List<T> getRoots() {
        return roots;
    }

	public boolean moveCategoryUp(int pos) {
		if (pos > 0 && pos < size()) {
			swap(pos, pos-1);
			return true;
		}
		return false;
	}

	public boolean moveCategoryDown(int pos) {
		if (pos >=0 && pos < size()-1) {
			swap(pos, pos+1);
			return true;
		}
		return false;
	}

	public boolean moveCategoryToTheTop(int pos) {
		if (pos > 0 && pos < size()) {
			T node = roots.remove(pos);
			roots.add(0, node);
			reIndex();
			return true;
		}
		return false;
	}

	public boolean moveCategoryToTheBottom(int pos) {
		if (pos >= 0 && pos < size()-1) {
			T node = roots.remove(pos);
			roots.add(size(), node);
			reIndex();
			return true;
		}
		return false;
	}

	public boolean sortByTitle() {
		sortByTitle(this);
		reIndex();
		return true;
	}
	
	private final Comparator<T> byTitleComparator = new Comparator<T>() {
		@Override
		public int compare(T c1, T c2) {
			String t1 = c1.title;
			String t2 = c2.title;
			if (t1 == null) {
				t1 = "";
			}
			if (t2 == null) {
				t2 = "";
			}
			return t1.compareTo(t2);
		}
	};

	private void sortByTitle(CategoryTree<T> tree) {
		Collections.sort(tree.roots, byTitleComparator);
		for (T node : tree) {
			if (node.hasChildren()) {
				sortByTitle(node.children);
			}
		}
	}

	private void swap(int from, int to) {
		T fromNode = roots.get(from);
		T toNode = roots.set(to, fromNode);
		roots.set(from, toNode);
		reIndex();
	}

	public void reIndex() {
		int left = Integer.MAX_VALUE;
		for (T node : roots) {
			if (node.left < left) {
				left = node.left;
			}
		}
		reIndex(this, left);
	}

	private int reIndex(CategoryTree<T> tree, int left) {
		for (T node : tree.roots) {
			node.left = left;
			if (node.hasChildren()) {
				node.right = reIndex(node.children, left+1);
			} else {
				node.right = left+1;
			}
			left = node.right+1;
		}			
		return left;
	}

}
