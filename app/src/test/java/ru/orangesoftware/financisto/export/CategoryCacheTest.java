package ru.orangesoftware.financisto.export;

import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ru.orangesoftware.financisto.model.Category;
import ru.orangesoftware.financisto.model.CategoryTree;
import ru.orangesoftware.financisto.test.CategoryBuilder;

import static org.junit.Assert.*;
import static ru.orangesoftware.financisto.export.CategoryCache.extractCategoryName;

public class CategoryCacheTest extends AbstractImportExportTest {

    CategoryCache cache = new CategoryCache();

    @Test
    public void should_split_category_name() {
        assertEquals("P1", extractCategoryName("P1"));
        assertEquals("P1:c1", extractCategoryName("P1:c1"));
        assertEquals("P1", extractCategoryName("P1/C2"));
        assertEquals("P1:c1", extractCategoryName("P1:c1/C2"));
    }

    @Test
    public void should_import_categories() {
        //given
        //P1         1-10
        // - cc1     2-7
        // -- c1     3-4
        // -- c2     5-6
        // - cc2     8-9
        //P2         11-14
        // - x1      12-13
        Set<CategoryInfo> list = new HashSet<>();
        list.add(new CategoryInfo("P1:cc1:c1", true));
        list.add(new CategoryInfo("P1:cc1", true));
        list.add(new CategoryInfo("P1:cc1:c2", true));
        list.add(new CategoryInfo("P2", false));
        list.add(new CategoryInfo("P2:x1", false));
        list.add(new CategoryInfo("P1", true));
        list.add(new CategoryInfo("P1:cc2", true));

        //when
        cache.insertCategories(db, list);

        //then
        assertNotNull(cache.findCategory("P1"));
        assertNotNull(cache.findCategory("P1:cc1"));
        assertNotNull(cache.findCategory("P1:cc1:c2"));
        assertNotNull(cache.findCategory("P2:x1"));

        Category noCategory = db.getCategoryWithParent(Category.NO_CATEGORY_ID);
        assertEquals(0, noCategory.left);
        assertEquals(15, noCategory.right);

        //then
        CategoryTree<Category> categories = db.getCategoriesTree(false);
        assertNotNull(categories);
        assertEquals(2, categories.size());

        Category c = categories.getAt(0);
        assertCategory("P1", true, c);
        assertEquals(2, c.children.size());

        assertCategory("cc1", true, c.children.getAt(0));
        assertEquals(2, c.children.getAt(0).children.size());

        assertCategory("cc2", true, c.children.getAt(1));
        assertFalse(c.children.getAt(1).hasChildren());

        c = categories.getAt(1);
        assertCategory("P2", false, c);
        assertEquals(1, c.children.size());

        assertCategory("x1", false, c.children.getAt(0));
    }

    @Test
    public void should_load_existing_categories() {
        //given existing
        /**
         * A
         * - A1
         * -- AA1
         * - A2
         * B
         */
        Map<String, Category> existingCategories = CategoryBuilder.createDefaultHierarchy(db);
        //when
        cache.loadExistingCategories(db);
        //then
        assertEquals(existingCategories.get("A").id, cache.findCategory("A").id);
        assertEquals(existingCategories.get("A1").id, cache.findCategory("A:A1").id);
        assertEquals(existingCategories.get("AA1").id, cache.findCategory("A:A1:AA1").id);
        assertEquals(existingCategories.get("A2").id, cache.findCategory("A:A2").id);
        assertEquals(existingCategories.get("B").id, cache.findCategory("B").id);
    }

    @Test
    public void should_merge_existing_and_new_categories() {
        //given existing
        CategoryBuilder.createDefaultHierarchy(db);

        //when
        cache.loadExistingCategories(db);
        Set<CategoryInfo> list = new HashSet<CategoryInfo>();
        list.add(new CategoryInfo("A:A1", true));
        list.add(new CategoryInfo("B", true));
        list.add(new CategoryInfo("A:A1:AA2", true));
        list.add(new CategoryInfo("A:A2:AB1", false));
        list.add(new CategoryInfo("C", false));
        list.add(new CategoryInfo("D:D1", true));
        cache.insertCategories(db, list);

        //then
        /**
         * A            1-12
         * - A1         2-7
         * -- AA1       3-4
         * -- AA2       5-6
         * - A2         8-11
         * -- AB1       9-10
         * B            13-14
         * C            15-16
         * D            17-20
         * - D1         18-19
         */
        CategoryTree<Category> categories = db.getCategoriesTree(false);
        assertNotNull(categories);
        assertEquals(4, categories.size());

        Category noCategory = db.getCategoryWithParent(Category.NO_CATEGORY_ID);
        assertEquals(0, noCategory.left);
        assertEquals(21, noCategory.right);

    }

}
