package ru.orangesoftware.financisto.model;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CategoryTreeTest {

    private CategoryTree<Category> tree;
    private Category a;

    @Before
    public void setUp() throws Exception {
        a = createIncomeCategory(1, 1, 8);
        a.title = "ZZZ";
        a.addChild(createCategory(2, 2, 3));
        a.addChild(createCategory(3, 4, 5));
        a.addChild(createCategory(4, 6, 7));
        Category b = createCategory(5, 9, 16);
        b.title = "YYY";
        Category b1 = createExpenseCategory(6, 11, 15);
        b.addChild(b1);
        b1.addChild(createCategory(7, 11, 12));
        b1.addChild(createCategory(8, 13, 14));
        Category c = createIncomeCategory(9, 17, 18);
        c.title = "XXX";
        tree = new CategoryTree<Category>();
        tree.add(a);
        tree.add(b);
        tree.add(c);
    }

    private Category createIncomeCategory(long id, int left, int right) {
        Category c = createCategory(id, left, right);
        c.makeThisCategoryIncome();
        return c;
    }

    private Category createExpenseCategory(long id, int left, int right) {
        Category c = createCategory(id, left, right);
        c.makeThisCategoryExpense();
        return c;
    }

    private Category createCategory(long id, int left, int right) {
        Category c = new Category(id);
        c.left = left;
        c.right = right;
        return c;
    }

    @Test
    public void shouldCheckThatAddingChildCategoryAutomaticallyPopulatesCorrectType() {
        assertTypesOfAllNodes();
    }

    private void assertTypesOfAllNodes() {
        for (Category c : tree) {
            assertTheSameTypeForAllChildren(c);
        }
    }

    private void assertTheSameTypeForAllChildren(Category parent) {
        if (parent.hasChildren()) {
            for (Category child : parent.children) {
                assertEquals("Parent and child should be of the same type", parent.type, child.type);
                assertTheSameTypeForAllChildren(child);
            }
        }
    }

    @Test
    public void shouldMoveCategoryWithNoChildrenUpCorrectly() {
        CategoryTree<Category> tree = a.children;
        assertFalse(tree.moveCategoryUp(tree.size()));
        assertFalse(tree.moveCategoryUp(-1));
        assertFalse(tree.moveCategoryUp(0));
        assertTrue(tree.moveCategoryUp(1));
        Category a2 = tree.getAt(0);
        assertEquals(3, a2.id);
        assertEquals(2, a2.left);
        assertEquals(3, a2.right);
        Category a1 = tree.getAt(1);
        assertEquals(2, a1.id);
        assertEquals(4, a1.left);
        assertEquals(5, a1.right);
        assertTypesOfAllNodes();
    }

    @Test
    public void shouldMoveCategoryWithNoChildrenDownCorrectly() {
        CategoryTree<Category> tree = a.children;
        assertFalse(tree.moveCategoryDown(tree.size()));
        assertFalse(tree.moveCategoryDown(tree.size() - 1));
        assertFalse(tree.moveCategoryDown(-1));
        assertTrue(tree.moveCategoryDown(0));
        Category a2 = tree.getAt(0);
        assertEquals(3, a2.id);
        assertEquals(2, a2.left);
        assertEquals(3, a2.right);
        Category a1 = tree.getAt(1);
        assertEquals(2, a1.id);
        assertEquals(4, a1.left);
        assertEquals(5, a1.right);
        assertTypesOfAllNodes();
    }

    @Test
    public void shouldMoveCategoryWithChildrenUpCorrectly() {
        CategoryTree<Category> tree = this.tree;
        assertTrue(tree.moveCategoryUp(1));
        Category a = tree.getAt(1);
        assertEquals(1, a.id);
        assertEquals(9, a.left);
        assertEquals(16, a.right);
        Category b = tree.getAt(0);
        assertEquals(5, b.id);
        assertEquals(1, b.left);
        assertEquals(8, b.right);
        Category b1 = b.children.getAt(0);
        assertEquals(6, b1.id);
        assertEquals(2, b1.left);
        assertEquals(7, b1.right);
        Category a1 = a.children.getAt(0);
        assertEquals(2, a1.id);
        assertEquals(10, a1.left);
        assertEquals(11, a1.right);
        assertTypesOfAllNodes();
    }

    @Test
    public void shouldMoveCategoryWithChildrenDownCorrectly() {
        CategoryTree<Category> tree = this.tree;
        assertTrue(tree.moveCategoryDown(0));
        Category a = tree.getAt(1);
        assertEquals(1, a.id);
        assertEquals(9, a.left);
        assertEquals(16, a.right);
        Category b = tree.getAt(0);
        assertEquals(5, b.id);
        assertEquals(1, b.left);
        assertEquals(8, b.right);
        Category b1 = b.children.getAt(0);
        assertEquals(6, b1.id);
        assertEquals(2, b1.left);
        assertEquals(7, b1.right);
        Category a1 = a.children.getAt(0);
        assertEquals(2, a1.id);
        assertEquals(10, a1.left);
        assertEquals(11, a1.right);
        assertTypesOfAllNodes();
    }

    @Test
    public void shouldMoveCategoryWithNoChildrenToTopCorrectly() {
        CategoryTree<Category> tree = a.children;
        assertFalse(tree.moveCategoryToTheTop(tree.size()));
        assertFalse(tree.moveCategoryToTheTop(-1));
        assertFalse(tree.moveCategoryToTheTop(0));
        assertTrue(tree.moveCategoryToTheTop(2));
        Category a3 = tree.getAt(0);
        assertEquals(4, a3.id);
        assertEquals(2, a3.left);
        assertEquals(3, a3.right);
        Category a1 = tree.getAt(1);
        assertEquals(2, a1.id);
        assertEquals(4, a1.left);
        assertEquals(5, a1.right);
        assertTypesOfAllNodes();
    }

    @Test
    public void shouldMoveCategoryWithNoChildrenToBottomCorrectly() {
        CategoryTree<Category> tree = a.children;
        assertFalse(tree.moveCategoryToTheBottom(tree.size()));
        assertFalse(tree.moveCategoryToTheBottom(tree.size() - 1));
        assertFalse(tree.moveCategoryToTheBottom(-1));
        assertTrue(tree.moveCategoryToTheBottom(1));
        Category a3 = tree.getAt(1);
        assertEquals(4, a3.id);
        assertEquals(4, a3.left);
        assertEquals(5, a3.right);
        Category a2 = tree.getAt(2);
        assertEquals(3, a2.id);
        assertEquals(6, a2.left);
        assertEquals(7, a2.right);
        assertTypesOfAllNodes();
    }

    @Test
    public void shouldMoveCategoryWithChildrenToTopCorrectly() {
        CategoryTree<Category> tree = this.tree;
        assertTrue(tree.moveCategoryToTheTop(1));
        Category a = tree.getAt(1);
        assertEquals(1, a.id);
        assertEquals(9, a.left);
        assertEquals(16, a.right);
        Category b = tree.getAt(0);
        assertEquals(5, b.id);
        assertEquals(1, b.left);
        assertEquals(8, b.right);
        Category b1 = b.children.getAt(0);
        assertEquals(6, b1.id);
        assertEquals(2, b1.left);
        assertEquals(7, b1.right);
        Category a1 = a.children.getAt(0);
        assertEquals(2, a1.id);
        assertEquals(10, a1.left);
        assertEquals(11, a1.right);
        assertTypesOfAllNodes();
    }

    @Test
    public void shouldMoveCategoryWithChildrenToBottomCorrectly() {
        CategoryTree<Category> tree = this.tree;
        assertTrue(tree.moveCategoryToTheBottom(0));
        Category a = tree.getAt(2);
        assertEquals(1, a.id);
        assertEquals(11, a.left);
        assertEquals(18, a.right);
        Category b = tree.getAt(0);
        assertEquals(5, b.id);
        assertEquals(1, b.left);
        assertEquals(8, b.right);
        Category b1 = b.children.getAt(0);
        assertEquals(6, b1.id);
        assertEquals(2, b1.left);
        assertEquals(7, b1.right);
        Category a1 = a.children.getAt(0);
        assertEquals(2, a1.id);
        assertEquals(12, a1.left);
        assertEquals(13, a1.right);
        assertTypesOfAllNodes();
    }

    @Test
    public void shouldSortByTitle() {
        CategoryTree<Category> tree = this.tree;
        assertTrue(tree.sortByTitle());
        Category c = tree.getAt(0);
        assertEquals(9, c.id);
        Category b = tree.getAt(1);
        assertEquals(5, b.id);
        Category a = tree.getAt(2);
        assertEquals(1, a.id);
        assertTypesOfAllNodes();
    }
}
