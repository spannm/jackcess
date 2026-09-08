/*
Copyright (c) 2008 Health Market Science, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.github.spannm.jackcess.impl;

import static io.github.spannm.jackcess.impl.IndexData.*;

import io.github.spannm.jackcess.JackcessRuntimeException;
import io.github.spannm.jackcess.impl.IndexData.DataPage;
import io.github.spannm.jackcess.impl.IndexData.Entry;
import io.github.spannm.jackcess.util.ToStringBuilder;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.*;

/**
 * Manager of the index pages for a IndexData.
 */
@SuppressWarnings("PMD.FieldDeclarationsShouldBeAtStartOfClass")
public class IndexPageCache {
    private enum UpdateType {
        ADD,
        REMOVE,
        REPLACE
    }

    /**
     * max number of pages to cache (unless a write operation is in progress)
     */
    private static final int                 MAX_CACHE_SIZE = 25;

    /** the index whose pages this cache is managing */
    private final IndexData                  indexData;
    /** the root page for the index */
    private DataPageMain                     rootPage;
    /** the currently loaded pages for this index, pageNumber -> page */
    private final Map<Integer, DataPageMain> dataPages     = new LinkedHashMap<>(16, 0.75f, true) {
        private static final long serialVersionUID = 0L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, DataPageMain> e) {
            // only purge when the size is too big and a
            // logical write operation is
            // not in progress (while an update is
            // happening, the pages can be in
            // flux and removing pages from the cache can
            // cause problems)
            if (size() > MAX_CACHE_SIZE && !getPageChannel().isWriting()) {
                purgeOldPages();
            }
            return false;
        }
    };
    /** the currently modified index pages */
    private final List<CacheDataPage>        modifiedPages = new ArrayList<>();

    public IndexPageCache(IndexData indexData) {
        this.indexData = indexData;
    }

    public IndexData getIndexData() {
        return indexData;
    }

    public PageChannel getPageChannel() {
        return getIndexData().getPageChannel();
    }

    /**
     * Sets the root page for this index, must be called before normal usage.
     *
     * @param pageNumber the root page number
     */
    public void setRootPageNumber(int pageNumber) throws IOException {
        rootPage = getDataPage(pageNumber);
        // root page has no parent
        rootPage.initParentPage(INVALID_INDEX_PAGE_NUMBER, false);
    }

    /**
     * Writes any outstanding changes for this index to the file.
     */
    public void write() throws IOException {
        // first discard any empty pages
        handleEmptyPages();
        // next, handle any necessary page splitting
        preparePagesForWriting();
        // finally, write all the modified pages (which are not being deleted)
        writeDataPages();
        // after we write everything, we can purge our cache if necessary
        if (dataPages.size() > MAX_CACHE_SIZE) {
            purgeOldPages();
        }
    }

    /**
     * Handles any modified pages which are empty as the first pass during a {@link #write} call. All empty pages are removed from the modifiedPages collection by this method.
     */
    private void handleEmptyPages() throws IOException {
        for (Iterator<CacheDataPage> iter = modifiedPages.iterator(); iter.hasNext();) {
            CacheDataPage cacheDataPage = iter.next();
            if (cacheDataPage.extra.entryView.isEmpty()) {
                if (!cacheDataPage.main.isRoot()) {
                    deleteDataPage(cacheDataPage);
                } else {
                    writeDataPage(cacheDataPage);
                }
                iter.remove();
            }
        }
    }

    /**
     * Prepares any non-empty modified pages for writing as the second pass
     * during a {@link #write} call. Updates entry prefixes, promotes/demotes
     * tail pages, and splits pages as needed.
     *
     * @throws IOException if an error occurs during page access or validation
     */
    private void preparePagesForWriting() throws IOException {
        boolean splitPages = false;
        int maxPageEntrySize = getIndexData().getMaxPageEntrySize();

        // we need to continue looping through all the pages until we do not split
        // any pages (because a split may cascade up the tree)
        do {
            splitPages = false;

            // we might be adding to this list while iterating, so we can't use an
            // iterator
            for (int i = 0; i < modifiedPages.size(); ++i) {

                CacheDataPage cacheDataPage = modifiedPages.get(i);

                if (!cacheDataPage.isLeaf()) {
                    // see if we need to update any child tail status
                    DataPageMain dpMain = cacheDataPage.main;
                    int size = cacheDataPage.extra.entryView.size();
                    if (dpMain.hasChildTail()) {
                        if (size == 1) {
                            demoteTail(cacheDataPage);
                        }
                    } else {
                        if (size > 1) {
                            // only a leaf page can become a tail page
                            DataPageMain lastChild = dpMain.getChildPage(cacheDataPage.extra.entryView.getLast());
                            if (lastChild.leaf) {
                                promoteTail(cacheDataPage, lastChild);
                            }
                        }
                    }
                }

                // look for pages with more entries than can fit on a page
                if (cacheDataPage.getTotalEntrySize() > maxPageEntrySize) {

                    // make sure the prefix is up-to-date (this may have gotten
                    // discarded by one of the update entry methods)
                    cacheDataPage.extra.updateEntryPrefix();

                    // now, see if the page will fit when compressed
                    if (cacheDataPage.getCompressedEntrySize() > maxPageEntrySize) {
                        // need to split this page
                        splitPages = true;
                        splitDataPage(cacheDataPage);
                    }
                }
            }

        } while (splitPages);
    }

    /**
     * Writes any non-empty modified pages as the last pass during a {@link #write} call. Clears the modifiedPages collection when finised.
     */
    private void writeDataPages() throws IOException {
        for (CacheDataPage cacheDataPage : modifiedPages) {
            if (cacheDataPage.extra.entryView.isEmpty()) {
                throw new IllegalStateException(withErrorContext("Unexpected empty page " + cacheDataPage));
            }
            writeDataPage(cacheDataPage);
        }
        modifiedPages.clear();
    }

    /**
     * Returns a CacheDataPage for the given page number, may be {@code null} if the given page number is invalid. Loads the given page if necessary.
     */
    public CacheDataPage getCacheDataPage(Integer pageNumber) throws IOException {
        DataPageMain main = getDataPage(pageNumber);
        return main != null ? new CacheDataPage(main) : null;
    }

    /**
     * Returns a DataPageMain for the given page number, may be {@code null} if the given page number is invalid. Loads the given page if necessary.
     */
    private DataPageMain getDataPage(Integer pageNumber) throws IOException {
        DataPageMain dataPage = dataPages.get(pageNumber);
        if (dataPage == null && pageNumber > INVALID_INDEX_PAGE_NUMBER) {
            dataPage = readDataPage(pageNumber).main;
            dataPages.put(pageNumber, dataPage);
        }
        return dataPage;
    }

    /**
     * Writes the given index page to the file.
     */
    private void writeDataPage(CacheDataPage cacheDataPage) throws IOException {
        getIndexData().writeDataPage(cacheDataPage);

        // lastly, mark the page as no longer modified
        cacheDataPage.extra.modified = false;
    }

    /**
     * Deletes the given index page from the file (clears the page).
     */
    private void deleteDataPage(CacheDataPage cacheDataPage) throws IOException {
        // free this database page
        getPageChannel().deallocatePage(cacheDataPage.main.pageNumber);

        // discard from our cache
        dataPages.remove(cacheDataPage.main.pageNumber);

        // lastly, mark the page as no longer modified
        cacheDataPage.extra.modified = false;
    }

    /**
     * Reads the given index page from the file.
     */
    private CacheDataPage readDataPage(Integer pageNumber) throws IOException {
        DataPageMain dataPage = new DataPageMain(pageNumber);
        DataPageExtra extra = new DataPageExtra();
        CacheDataPage cacheDataPage = new CacheDataPage(dataPage, extra);
        getIndexData().readDataPage(cacheDataPage);

        // associate the extra info with the main data page
        dataPage.setExtra(extra);

        return cacheDataPage;
    }

    /**
     * Removes the entry with the given index from the given page.
     *
     * @param cacheDataPage the page from which to remove the entry
     * @param entryIdx the index of the entry to remove
     */
    private Entry removeEntry(CacheDataPage cacheDataPage, int entryIdx) throws IOException {
        return updateEntry(cacheDataPage, entryIdx, null, UpdateType.REMOVE);
    }

    /**
     * Adds the entry to the given page at the given index.
     *
     * @param cacheDataPage the page to which to add the entry
     * @param entryIdx the index at which to add the entry
     * @param newEntry the entry to add
     */
    private void addEntry(CacheDataPage cacheDataPage, int entryIdx, Entry newEntry) throws IOException {
        updateEntry(cacheDataPage, entryIdx, newEntry, UpdateType.ADD);
    }

    /**
     * Updates the entries on the given page according to the given updateType.
     *
     * @param cacheDataPage the page to update
     * @param entryIdx the index at which to add/remove/replace the entry
     * @param newEntry the entry to add/replace
     * @param upType the type of update to make
     */
    private Entry updateEntry(CacheDataPage cacheDataPage, int entryIdx, Entry newEntry, UpdateType upType) throws IOException {
        DataPageMain dpMain = cacheDataPage.main;
        DataPageExtra dpExtra = cacheDataPage.extra;

        if (newEntry != null) {
            validateEntryForPage(dpMain, newEntry);
        }

        // note, it's slightly ucky, but we need to load the parent page before we
        // start mucking with our entries because our parent may use our entries.
        CacheDataPage parentDataPage = !dpMain.isRoot() ? new CacheDataPage(dpMain.getParentPage()) : null;

        Entry oldLastEntry = dpExtra.entryView.getLast();
        Entry oldEntry = null;
        int entrySizeDiff = 0;

        switch (upType) {
            case ADD:
                dpExtra.entryView.add(entryIdx, newEntry);
                entrySizeDiff += newEntry.size();
                break;

            case REPLACE:
                oldEntry = dpExtra.entryView.set(entryIdx, newEntry);
                entrySizeDiff += newEntry.size() - oldEntry.size();
                break;

            case REMOVE:
                oldEntry = dpExtra.entryView.remove(entryIdx);
                entrySizeDiff -= oldEntry.size();
                break;

            default:
                throw new JackcessRuntimeException(withErrorContext("unknown update type " + upType));
        }

        boolean updateLast = oldLastEntry != dpExtra.entryView.getLast();

        // child tail entry updates do not modify the page
        if (!updateLast || !dpMain.hasChildTail()) {
            dpExtra.totalEntrySize += entrySizeDiff;
            setModified(cacheDataPage);

            // for now, just clear the prefix, we'll fix it later
            dpExtra.entryPrefix = EMPTY_PREFIX;
        }

        if (dpExtra.entryView.isEmpty()) {
            // this page is dead
            removeDataPage(parentDataPage, cacheDataPage, oldLastEntry);
            return oldEntry;
        }

        // determine if we need to update our parent page
        if (!updateLast || dpMain.isRoot()) {
            // no parent
            return oldEntry;
        }

        // the update to the last entry needs to be propagated to our parent
        replaceParentEntry(parentDataPage, cacheDataPage, oldLastEntry);
        return oldEntry;
    }

    /**
     * Removes an index page which has become empty. If this page is the root page, just clears it.
     *
     * @param parentDataPage the parent of the removed page
     * @param cacheDataPage the page to remove
     * @param oldLastEntry the last entry for this page (before it was removed)
     */
    private void removeDataPage(CacheDataPage parentDataPage, CacheDataPage cacheDataPage, Entry oldLastEntry) throws IOException {
        DataPageMain dpMain = cacheDataPage.main;
        DataPageExtra dpExtra = cacheDataPage.extra;

        if (dpMain.hasChildTail()) {
            throw new IllegalStateException(withErrorContext("Still has child tail?"));
        }

        if (dpExtra.totalEntrySize != 0) {
            throw new IllegalStateException(withErrorContext("Empty page but size is not 0? " + dpExtra.totalEntrySize + ", " + cacheDataPage));
        }

        if (dpMain.isRoot()) {
            // clear out this page (we don't actually remove it)
            dpExtra.entryPrefix = EMPTY_PREFIX;
            // when the root page becomes empty, it becomes a leaf page again
            dpMain.leaf = true;
            return;
        }

        // remove this page from its parent page
        updateParentEntry(parentDataPage, cacheDataPage, oldLastEntry, null, UpdateType.REMOVE);

        // remove this page from any next/prev pages
        removeFromPeers(cacheDataPage);
    }

    /**
     * Removes a now empty index page from its next and previous peers.
     *
     * @param cacheDataPage the page to remove
     */
    private void removeFromPeers(CacheDataPage cacheDataPage) throws IOException {
        DataPageMain dpMain = cacheDataPage.main;

        Integer prevPageNumber = dpMain.prevPageNumber;
        Integer nextPageNumber = dpMain.nextPageNumber;

        DataPageMain prevMain = dpMain.getPrevPage();
        if (prevMain != null) {
            setModified(new CacheDataPage(prevMain));
            prevMain.nextPageNumber = nextPageNumber;
        }

        DataPageMain nextMain = dpMain.getNextPage();
        if (nextMain != null) {
            setModified(new CacheDataPage(nextMain));
            nextMain.prevPageNumber = prevPageNumber;
        }
    }

    /**
     * Adds an entry for the given child page to the given parent page.
     *
     * @param parentDataPage the parent page to which to add the entry
     * @param childDataPage the child from which to get the entry to add
     */
    private void addParentEntry(CacheDataPage parentDataPage, CacheDataPage childDataPage) throws IOException {
        DataPageExtra childExtra = childDataPage.extra;
        updateParentEntry(parentDataPage, childDataPage, null, childExtra.entryView.getLast(), UpdateType.ADD);
    }

    /**
     * Replaces the entry for the given child page in the given parent page.
     *
     * @param parentDataPage the parent page in which to replace the entry
     * @param childDataPage the child for which the entry is being replaced
     * @param oldEntry the old child entry for the child page
     */
    private void replaceParentEntry(CacheDataPage parentDataPage, CacheDataPage childDataPage, Entry oldEntry) throws IOException {
        DataPageExtra childExtra = childDataPage.extra;
        updateParentEntry(parentDataPage, childDataPage, oldEntry, childExtra.entryView.getLast(), UpdateType.REPLACE);
    }

    /**
     * Updates the entry for the given child page in the given parent page according to the given updateType.
     *
     * @param parentDataPage the parent page in which to update the entry
     * @param childDataPage the child for which the entry is being updated
     * @param oldEntry the old child entry to remove/replace
     * @param newEntry the new child entry to replace/add
     * @param upType the type of update to make
     */
    private void updateParentEntry(CacheDataPage parentDataPage, CacheDataPage childDataPage, Entry oldEntry, Entry newEntry, UpdateType upType) throws IOException {
        DataPageMain childMain = childDataPage.main;
        DataPageExtra parentExtra = parentDataPage.extra;

        if (childMain.isTail() && upType != UpdateType.REMOVE) {
            // for add or replace, update the child tail info before updating the
            // parent entries
            updateParentTail(parentDataPage, childDataPage, upType);
        }

        if (oldEntry != null) {
            oldEntry = oldEntry.asNodeEntry(childMain.pageNumber);
        }
        if (newEntry != null) {
            newEntry = newEntry.asNodeEntry(childMain.pageNumber);
        }

        boolean expectFound = true;
        int idx = 0;

        switch (upType) {
            case ADD:
                expectFound = false;
                idx = parentExtra.entryView.find(newEntry);
                break;

            case REPLACE:
            case REMOVE:
                idx = parentExtra.entryView.find(oldEntry);
                break;

            default:
                throw new JackcessRuntimeException(withErrorContext("unknown update type " + upType));
        }

        if (idx < 0) {
            if (expectFound) {
                throw new IllegalStateException(withErrorContext("Could not find child entry in parent; childEntry " + oldEntry + "; parent " + parentDataPage));
            }
            idx = missingIndexToInsertionPoint(idx);
        } else {
            if (!expectFound) {
                throw new IllegalStateException(withErrorContext("Unexpectedly found child entry in parent; childEntry " + newEntry + "; parent " + parentDataPage));
            }
        }
        updateEntry(parentDataPage, idx, newEntry, upType);

        if (childMain.isTail() && upType == UpdateType.REMOVE) {
            // for remove, update the child tail info after updating the parent
            // entries
            updateParentTail(parentDataPage, childDataPage, upType);
        }
    }

    /**
     * Updates the child tail info in the given parent page according to the given updateType.
     *
     * @param parentDataPage the parent page in which to update the child tail
     * @param childDataPage the child to add/replace
     * @param upType the type of update to make
     */
    private void updateParentTail(CacheDataPage parentDataPage, CacheDataPage childDataPage, UpdateType upType) {
        DataPageMain parentMain = parentDataPage.main;

        int newChildTailPageNumber = upType == UpdateType.REMOVE ? INVALID_INDEX_PAGE_NUMBER : childDataPage.main.pageNumber;
        if (!parentMain.isChildTailPageNumber(newChildTailPageNumber)) {
            setModified(parentDataPage);
            parentMain.childTailPageNumber = newChildTailPageNumber;
        }
    }

    /**
     * Verifies that the given entry type (node/leaf) is valid for the given page (node/leaf).
     *
     * @param dpMain the page to which the entry will be added
     * @param entry the entry being added
     * @throws IllegalStateException if the entry type does not match the page type
     */
    private void validateEntryForPage(DataPageMain dpMain, Entry entry) {
        if (dpMain.leaf != entry.isLeafEntry()) {
            throw new IllegalStateException(withErrorContext("Trying to update page with wrong entry type; pageLeaf " + dpMain.leaf + ", entryLeaf " + entry.isLeafEntry()));
        }
    }

    /**
     * Splits an index page which has too many entries on it.
     *
     * @param origDataPage the page to split
     */
    private void splitDataPage(CacheDataPage origDataPage) throws IOException {
        DataPageMain origMain = origDataPage.main;
        DataPageExtra origExtra = origDataPage.extra;

        setModified(origDataPage);

        int numEntries = origExtra.entries.size();
        if (numEntries < 2) {
            throw new IllegalStateException(withErrorContext("Cannot split page with less than 2 entries " + origDataPage));
        }

        if (origMain.isRoot()) {
            // we can't split the root page directly, so we need to put another page
            // between the root page and its sub-pages, and then split that page.
            CacheDataPage newDataPage = nestRootDataPage(origDataPage);

            // now, split this new page instead
            origDataPage = newDataPage;
            origMain = newDataPage.main;
            origExtra = newDataPage.extra;
        }

        // note, it's slightly ucky, but we need to load the parent page before we
        // start mucking with our entries because our parent may use our entries.
        DataPageMain parentMain = origMain.getParentPage();
        CacheDataPage parentDataPage = new CacheDataPage(parentMain);

        // note, there are many, many ways this could be improved/tweaked. for
        // now, we just want it to be functional...
        // so, we will naively move half the entries from one page to a new page.

        CacheDataPage newDataPage = allocateNewCacheDataPage(parentMain.pageNumber, origMain.leaf);
        DataPageMain newMain = newDataPage.main;
        DataPageExtra newExtra = newDataPage.extra;

        List<Entry> headEntries = origExtra.entries.subList(0, (numEntries + 1) / 2);

        // move first half of the entries from old page to new page (so we do not
        // need to muck with any tail entries)
        for (Entry headEntry : headEntries) {
            newExtra.totalEntrySize += headEntry.size();
            newExtra.entries.add(headEntry);
        }
        newExtra.setEntryView(newMain);

        // remove the moved entries from the old page
        headEntries.clear();
        origExtra.entryPrefix = EMPTY_PREFIX;
        origExtra.totalEntrySize -= newExtra.totalEntrySize;

        // insert this new page between the old page and any previous page
        addToPeersBefore(newDataPage, origDataPage);

        if (!newMain.leaf) {
            // reparent the children pages of the new page
            reparentChildren(newDataPage);

            // if the children of this page are also node pages, then the next/prev
            // links should not cross parent boundaries (the leaf pages are linked
            // from beginning to end, but child node pages are only linked within
            // the same parent)
            DataPageMain childMain = newMain.getChildPage(newExtra.entryView.getLast());
            if (!childMain.leaf) {
                separateFromNextPeer(new CacheDataPage(childMain));
            }
        }

        // lastly, we need to add the new page to the parent page's entries
        addParentEntry(parentDataPage, newDataPage);
    }

    /**
     * Copies the current root page info into a new page and nests this page under the root page. This must be done when the root page needs to be split.
     *
     * @param rootDataPage the root data page
     *
     * @return the newly created page nested under the root page
     */
    private CacheDataPage nestRootDataPage(CacheDataPage rootDataPage) throws IOException {
        DataPageMain rootMain = rootDataPage.main;
        DataPageExtra rootExtra = rootDataPage.extra;

        if (!rootMain.isRoot()) {
            throw new IllegalArgumentException(withErrorContext("should be called with root, duh"));
        }

        CacheDataPage newDataPage = allocateNewCacheDataPage(rootMain.pageNumber, rootMain.leaf);
        DataPageMain newMain = newDataPage.main;
        DataPageExtra newExtra = newDataPage.extra;

        // move entries to new page
        newMain.childTailPageNumber = rootMain.childTailPageNumber;
        newExtra.entries = rootExtra.entries;
        newExtra.entryPrefix = rootExtra.entryPrefix;
        newExtra.totalEntrySize = rootExtra.totalEntrySize;
        newExtra.setEntryView(newMain);

        if (!newMain.leaf) {
            // we need to re-parent all the child pages
            reparentChildren(newDataPage);
        }

        // clear the root page
        rootMain.leaf = false;
        rootMain.childTailPageNumber = INVALID_INDEX_PAGE_NUMBER;
        rootExtra.entries = new ArrayList<>();
        rootExtra.entryPrefix = EMPTY_PREFIX;
        rootExtra.totalEntrySize = 0;
        rootExtra.setEntryView(rootMain);

        // add the new page as the first child of the root page
        addParentEntry(rootDataPage, newDataPage);

        return newDataPage;
    }

    /**
     * Allocates a new index page with the given parent page and type.
     *
     * @param parentPageNumber the parent page for the new page
     * @param isLeaf whether or not the new page is a leaf page
     *
     * @return the newly created page
     */
    private CacheDataPage allocateNewCacheDataPage(Integer parentPageNumber, boolean isLeaf) throws IOException {
        DataPageMain dpMain = new DataPageMain(getPageChannel().allocateNewPage());
        DataPageExtra dpExtra = new DataPageExtra();
        dpMain.initParentPage(parentPageNumber, false);
        dpMain.leaf = isLeaf;
        dpMain.prevPageNumber = INVALID_INDEX_PAGE_NUMBER;
        dpMain.nextPageNumber = INVALID_INDEX_PAGE_NUMBER;
        dpMain.childTailPageNumber = INVALID_INDEX_PAGE_NUMBER;
        dpExtra.entries = new ArrayList<>();
        dpExtra.entryPrefix = EMPTY_PREFIX;
        dpMain.setExtra(dpExtra);

        // add to our page cache
        dataPages.put(dpMain.pageNumber, dpMain);

        // update owned pages cache
        indexData.addOwnedPage(dpMain.pageNumber);

        // needs to be written out
        CacheDataPage cacheDataPage = new CacheDataPage(dpMain, dpExtra);
        setModified(cacheDataPage);

        return cacheDataPage;
    }

    /**
     * Inserts the new page as a peer between the given original page and any previous peer page.
     *
     * @param newDataPage the new index page
     * @param origDataPage the current index page
     */
    private void addToPeersBefore(CacheDataPage newDataPage, CacheDataPage origDataPage) throws IOException {
        DataPageMain origMain = origDataPage.main;
        DataPageMain newMain = newDataPage.main;

        DataPageMain prevMain = origMain.getPrevPage();

        newMain.nextPageNumber = origMain.pageNumber;
        newMain.prevPageNumber = origMain.prevPageNumber;
        origMain.prevPageNumber = newMain.pageNumber;

        if (prevMain != null) {
            setModified(new CacheDataPage(prevMain));
            prevMain.nextPageNumber = newMain.pageNumber;
        }
    }

    /**
     * Separates the given index page from any next peer page.
     *
     * @param cacheDataPage the index page to be separated
     */
    private void separateFromNextPeer(CacheDataPage cacheDataPage) throws IOException {
        DataPageMain dpMain = cacheDataPage.main;

        setModified(cacheDataPage);

        DataPageMain nextMain = dpMain.getNextPage();
        setModified(new CacheDataPage(nextMain));

        nextMain.prevPageNumber = INVALID_INDEX_PAGE_NUMBER;
        dpMain.nextPageNumber = INVALID_INDEX_PAGE_NUMBER;
    }

    /**
     * Sets the parent info for the children of the given page to the given page.
     *
     * @param cacheDataPage the page whose children need to be updated
     */
    private void reparentChildren(CacheDataPage cacheDataPage) {
        DataPageMain dpMain = cacheDataPage.main;
        DataPageExtra dpExtra = cacheDataPage.extra;

        // note, the "parent" page number is not actually persisted, so we do not
        // need to mark any updated pages as modified. for the same reason, we
        // don't need to load the pages if not already loaded
        for (Entry entry : dpExtra.entryView) {
            Integer childPageNumber = entry.getSubPageNumber();
            DataPageMain childMain = dataPages.get(childPageNumber);
            if (childMain != null) {
                childMain.setParentPage(dpMain.pageNumber, dpMain.isChildTailPageNumber(childPageNumber));
            }
        }
    }

    /**
     * Makes the tail entry of the given page a normal entry on that page, done when there is only one entry left on a page, and it is the tail.
     *
     * @param cacheDataPage the page whose tail must be updated
     */
    private void demoteTail(CacheDataPage cacheDataPage) throws IOException {
        // there's only one entry on the page, and it's the tail. make it a
        // normal entry
        DataPageMain dpMain = cacheDataPage.main;
        DataPageExtra dpExtra = cacheDataPage.extra;

        setModified(cacheDataPage);

        DataPageMain tailMain = dpMain.getChildTailPage();
        CacheDataPage tailDataPage = new CacheDataPage(tailMain);

        // move the tail entry to the last normal entry
        updateParentTail(cacheDataPage, tailDataPage, UpdateType.REMOVE);
        Entry tailEntry = dpExtra.entryView.demoteTail();
        dpExtra.totalEntrySize += tailEntry.size();
        dpExtra.entryPrefix = EMPTY_PREFIX;

        tailMain.setParentPage(dpMain.pageNumber, false);
    }

    /**
     * Makes the last normal entry of the given page the tail entry on that page, done when there are multiple entries on a page and no tail entry.
     * <p>
     * This is used during index updates to ensure that a non-leaf page is not
     * accidentally treated as a tail page. It updates the parent references
     * and ensures the page is correctly positioned in the internal cache.
     *
     * @param cachePage the cache data page being processed
     * @param tailPage the main data page to be promoted
     * @throws IOException if the promotion fails due to I/O errors
     */
    private void promoteTail(CacheDataPage cacheDataPage, DataPageMain lastMain) throws IOException {
        // there's not tail currently on this page, make last entry a tail
        DataPageMain dpMain = cacheDataPage.main;
        DataPageExtra dpExtra = cacheDataPage.extra;

        setModified(cacheDataPage);

        CacheDataPage lastDataPage = new CacheDataPage(lastMain);

        // move the "last" normal entry to the tail entry
        updateParentTail(cacheDataPage, lastDataPage, UpdateType.ADD);
        Entry lastEntry = dpExtra.entryView.promoteTail();
        dpExtra.totalEntrySize -= lastEntry.size();
        dpExtra.entryPrefix = EMPTY_PREFIX;

        lastMain.setParentPage(dpMain.pageNumber, true);
    }

    /**
     * Finds the index page on which the given entry does or should reside.
     *
     * @param e the entry to find
     */
    public CacheDataPage findCacheDataPage(Entry e) throws IOException {
        DataPageMain curPage = rootPage;
        while (true) {

            if (curPage.leaf) {
                // nowhere to go from here
                return new CacheDataPage(curPage);
            }

            DataPageExtra extra = curPage.getExtra();

            // need to descend
            int idx = extra.entryView.find(e);
            if (idx < 0) {
                idx = missingIndexToInsertionPoint(idx);
                if (idx == extra.entryView.size()) {
                    // just move to last child page
                    idx--;
                }
            }

            Entry nodeEntry = extra.entryView.get(idx);
            curPage = curPage.getChildPage(nodeEntry);
        }
    }

    /**
     * Marks the given index page as modified and saves it for writing, if necessary (if the page is already marked, does nothing).
     *
     * @param cacheDataPage the modified index page
     */
    private void setModified(CacheDataPage cacheDataPage) {
        if (!cacheDataPage.extra.modified) {
            modifiedPages.add(cacheDataPage);
            cacheDataPage.extra.modified = true;
        }
    }

    /**
     * Finds the valid entry prefix given the first/last entries on an index page.
     *
     * @param e1 the first entry on the page
     * @param e2 the last entry on the page
     *
     * @return a valid entry prefix for the page
     */
    private static byte[] findCommonPrefix(Entry e1, Entry e2) {
        byte[] b1 = e1.getEntryBytes();
        byte[] b2 = e2.getEntryBytes();

        int maxLen = b1.length;
        byte[] prefix = b1;
        if (b1.length > b2.length) {
            maxLen = b2.length;
            prefix = b2;
        }

        int len = 0;
        while (len < maxLen && b1[len] == b2[len]) {
            len++;
        }

        if (len < prefix.length) {
            if (len == 0) {
                return EMPTY_PREFIX;
            }

            // need new prefix
            prefix = ByteUtil.copyOf(prefix, len);
        }

        return prefix;
    }

    /**
     * Used by unit tests to validate the internal status of the index.
     */
    void validate(boolean forceLoad) throws IOException {
        new Validator(forceLoad).validate();
    }

    /**
     * Collects all the cache pages in the cache.
     *
     * @param pages the List to update
     * @param dpMain the index page to collect
     */
    private List<Object> collectPages(List<Object> pages, DataPageMain dpMain) {
        try {
            CacheDataPage cacheDataPage = new CacheDataPage(dpMain);
            pages.add(cacheDataPage);
            if (!dpMain.leaf) {
                for (Entry e : cacheDataPage.extra.entryView) {
                    DataPageMain childMain = dpMain.getChildPage(e);
                    collectPages(pages, childMain);
                }
            }
        } catch (IOException _ex) {
            pages.add("DataPage[" + dpMain.pageNumber + "]: <" + _ex + ">");
        }
        return pages;
    }

    /**
     * Trims the size of the dataPages cache appropriately (assuming caller has already verified that the cache needs trimming).
     */
    private void purgeOldPages() {
        Iterator<DataPageMain> iter = dataPages.values().iterator();
        while (iter.hasNext()) {
            DataPageMain dpMain = iter.next();
            // note, we never purge the root page
            if (dpMain != rootPage) {
                iter.remove();
                if (dataPages.size() <= MAX_CACHE_SIZE) {
                    break;
                }
            }
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.builder(this).append("pages", rootPage == null ? "(uninitialized)" : collectPages(new ArrayList<>(), rootPage)).toString();
    }

    private String withErrorContext(String msg) {
        return indexData.withErrorContext(msg);
    }

    /**
     * Keeps track of the main info for an index page.
     */
    private class DataPageMain {
        public final int                 pageNumber;
        public Integer                   prevPageNumber;
        public Integer                   nextPageNumber;
        public Integer                   childTailPageNumber;
        public Integer                   parentPageNumber;
        public boolean                   leaf;
        public boolean                   tail;
        private Reference<DataPageExtra> extra;

        private DataPageMain(int pageNumber) {
            this.pageNumber = pageNumber;
        }

        public IndexPageCache getCache() {
            return IndexPageCache.this;
        }

        public boolean isRoot() {
            return this == rootPage;
        }

        public boolean isTail() throws IOException {
            resolveParent();
            return tail;
        }

        public boolean hasChildTail() {
            return childTailPageNumber != INVALID_INDEX_PAGE_NUMBER;
        }

        public boolean isChildTailPageNumber(int checkPageNumber) {
            return childTailPageNumber == checkPageNumber;
        }

        public DataPageMain getParentPage() throws IOException {
            resolveParent();
            return getDataPage(parentPageNumber);
        }

        public void initParentPage(Integer newParentPageNumber, boolean isTail) {
            // only set if not already set
            if (parentPageNumber == null) {
                setParentPage(newParentPageNumber, isTail);
            }
        }

        public void setParentPage(Integer newParentPageNumber, boolean isTail) {
            parentPageNumber = newParentPageNumber;
            tail = isTail;
        }

        public DataPageMain getPrevPage() throws IOException {
            return getDataPage(prevPageNumber);
        }

        public DataPageMain getNextPage() throws IOException {
            return getDataPage(nextPageNumber);
        }

        public DataPageMain getChildPage(Entry e) throws IOException {
            Integer childPageNumber = e.getSubPageNumber();
            return getChildPage(childPageNumber, isChildTailPageNumber(childPageNumber));
        }

        public DataPageMain getChildTailPage() throws IOException {
            return getChildPage(childTailPageNumber, true);
        }

        /**
         * Returns a child page for the given page number, updating its parent info if necessary.
         */
        private DataPageMain getChildPage(Integer childPageNumber, boolean isTail) throws IOException {
            DataPageMain child = getDataPage(childPageNumber);
            if (child != null) {
                // set the parent info for this child (if necessary)
                child.initParentPage(pageNumber, isTail);
            }
            return child;
        }

        public DataPageExtra getExtra() throws IOException {
            DataPageExtra curExtra = extra.get();
            if (curExtra == null) {
                curExtra = readDataPage(pageNumber).extra;
                setExtra(curExtra);
            }

            return curExtra;
        }

        public void setExtra(DataPageExtra extra) throws IOException {
            extra.setEntryView(this);
            this.extra = new SoftReference<>(extra);
        }

        private void resolveParent() throws IOException {
            if (parentPageNumber == null) {
                // the act of searching for the last entry should resolve any parent
                // pages along the path
                findCacheDataPage(getExtra().entryView.getLast());
                if (parentPageNumber == null) {
                    throw new IllegalStateException(withErrorContext("Parent was not resolved"));
                }
            }
        }

        @Override
        public String toString() {
            return (leaf ? "Leaf" : "Node") + "DPMain[" + pageNumber + "] " + prevPageNumber + ", " + nextPageNumber + ", (" + childTailPageNumber + ")";
        }
    }

    /**
     * Keeps track of the extra info for an index page. This info (if unmodified) may be re-read from disk as necessary.
     */
    private static class DataPageExtra {
        /**
         * sorted collection of index entries. this is kept in a list instead of a SortedSet because the SortedSet has lame traversal utilities
         */
        public List<Entry>   entries;
        public EntryListView entryView;
        public byte[]        entryPrefix;
        public int           totalEntrySize;
        public boolean       modified;

        public void setEntryView(DataPageMain main) throws IOException {
            entryView = new EntryListView(main, this);
        }

        public void updateEntryPrefix() {
            if (entryPrefix.length == 0) {
                // prefix is only related to *real* entries, tail not included
                entryPrefix = findCommonPrefix(entries.get(0), entries.get(entries.size() - 1));
            }
        }

        @Override
        public String toString() {
            return ToStringBuilder.builder("DPExtra").append(null, entryView).toString();
        }
    }

    /**
     * IndexPageCache implementation of an Index {@link DataPage}.
     */
    private static final class CacheDataPage extends DataPage {
        public final DataPageMain  main;
        public final DataPageExtra extra;

        private CacheDataPage(DataPageMain dataPage) throws IOException {
            this(dataPage, dataPage.getExtra());
        }

        private CacheDataPage(DataPageMain dataPage, DataPageExtra extra) {
            main = dataPage;
            this.extra = extra;
        }

        @Override
        public int getPageNumber() {
            return main.pageNumber;
        }

        @Override
        public boolean isLeaf() {
            return main.leaf;
        }

        @Override
        public void setLeaf(boolean isLeaf) {
            main.leaf = isLeaf;
        }

        @Override
        public int getPrevPageNumber() {
            return main.prevPageNumber;
        }

        @Override
        public void setPrevPageNumber(int pageNumber) {
            main.prevPageNumber = pageNumber;
        }

        @Override
        public int getNextPageNumber() {
            return main.nextPageNumber;
        }

        @Override
        public void setNextPageNumber(int pageNumber) {
            main.nextPageNumber = pageNumber;
        }

        @Override
        public int getChildTailPageNumber() {
            return main.childTailPageNumber;
        }

        @Override
        public void setChildTailPageNumber(int pageNumber) {
            main.childTailPageNumber = pageNumber;
        }

        @Override
        public int getTotalEntrySize() {
            return extra.totalEntrySize;
        }

        @Override
        public void setTotalEntrySize(int totalSize) {
            extra.totalEntrySize = totalSize;
        }

        @Override
        public byte[] getEntryPrefix() {
            return extra.entryPrefix;
        }

        @Override
        public void setEntryPrefix(byte[] entryPrefix) {
            extra.entryPrefix = entryPrefix;
        }

        @Override
        public List<Entry> getEntries() {
            return extra.entries;
        }

        @Override
        public void setEntries(List<Entry> entries) {
            extra.entries = entries;
        }

        @Override
        public void addEntry(int idx, Entry entry) throws IOException {
            main.getCache().addEntry(this, idx, entry);
        }

        @Override
        public Entry removeEntry(int idx) throws IOException {
            return main.getCache().removeEntry(this, idx);
        }

    }

    /**
     * A view of an index page's entries which combines the normal entries and tail entry into one collection.
     */
    private static class EntryListView extends AbstractList<Entry> implements RandomAccess {
        private final DataPageExtra extra;
        private Entry               childTailEntry;

        private EntryListView(DataPageMain main, DataPageExtra extra) throws IOException {
            if (main.hasChildTail()) {
                childTailEntry = main.getChildTailPage().getExtra().entryView.getLast().asNodeEntry(main.childTailPageNumber);
            }
            this.extra = extra;
        }

        private List<Entry> getEntries() {
            return extra.entries;
        }

        @Override
        public int size() {
            int size = getEntries().size();
            if (hasChildTail()) {
                size++;
            }
            return size;
        }

        @Override
        public Entry get(int idx) {
            return isCurrentChildTailIndex(idx) ? childTailEntry : getEntries().get(idx);
        }

        @Override
        public Entry set(int idx, Entry newEntry) {
            return isCurrentChildTailIndex(idx) ? withChildTailEntry(newEntry) : getEntries().set(idx, newEntry);
        }

        @Override
        public void add(int idx, Entry newEntry) {
            // note, we will never add to the "tail" entry, that will always be
            // handled through promoteTail
            getEntries().add(idx, newEntry);
        }

        @Override
        public Entry remove(int idx) {
            return isCurrentChildTailIndex(idx) ? withChildTailEntry(null) : getEntries().remove(idx);
        }

        public Entry withChildTailEntry(Entry newEntry) {
            Entry old = childTailEntry;
            childTailEntry = newEntry;
            return old;
        }

        private boolean hasChildTail() {
            return childTailEntry != null;
        }

        private boolean isCurrentChildTailIndex(int idx) {
            return idx == getEntries().size();
        }

        @SuppressWarnings("PMD.MissingOverride")
        public Entry getLast() {
            return hasChildTail() ? childTailEntry : !getEntries().isEmpty() ? getEntries().get(getEntries().size() - 1) : null;
        }

        public Entry demoteTail() {
            Entry tail = childTailEntry;
            childTailEntry = null;
            getEntries().add(tail);
            return tail;
        }

        public Entry promoteTail() {
            Entry last = getEntries().remove(getEntries().size() - 1);
            childTailEntry = last;
            return last;
        }

        public int find(Entry e) {
            return Collections.binarySearch(this, e);
        }

    }

    /**
     * Utility class for running index validation.
     */
    private final class Validator {
        private final boolean                    forceLoad;
        private final Map<Integer, DataPageMain> knownPages   = new HashMap<>();
        private final Queue<DataPageMain>        pendingPages = new LinkedList<>();

        private Validator(boolean forceLoad) {
            this.forceLoad = forceLoad;
            knownPages.putAll(dataPages);
            pendingPages.addAll(knownPages.values());
        }

        void validate() throws IOException {
            DataPageMain dpMain = null;
            while ((dpMain = pendingPages.poll()) != null) {
                DataPageExtra dpExtra = dpMain.getExtra();
                validateEntries(dpExtra);
                validateChildren(dpMain, dpExtra);
                validatePeers(dpMain);
            }
        }

        /**
         * Validates the entries for an index page
         *
         * @param dpExtra the entries to validate
         */
        private void validateEntries(DataPageExtra dpExtra) throws IOException {
            int entrySize = 0;
            Entry prevEntry = FIRST_ENTRY;
            for (Entry e : dpExtra.entries) {
                entrySize += e.size();
                if (prevEntry.compareTo(e) >= 0) {
                    throw new IOException(withErrorContext("Unexpected order in index entries, " + prevEntry + " >= " + e));
                }
                prevEntry = e;
            }

            if (dpExtra.entryView.hasChildTail()) {
                Entry tailE = dpExtra.entryView.getLast();
                if (prevEntry.compareTo(tailE) >= 0) {
                    throw new IOException(
                        withErrorContext("Unexpected order in index entries, " + prevEntry + " >= " + tailE));
                }
            }

            if (entrySize != dpExtra.totalEntrySize) {
                throw new IllegalStateException(withErrorContext("Expected size " + entrySize + " but was " + dpExtra.totalEntrySize));
            }
        }

        /**
         * Validates the children for an index page
         *
         * @param dpMain the index page
         * @param dpExtra the child entries to validate
         */
        private void validateChildren(DataPageMain dpMain, DataPageExtra dpExtra) throws IOException {
            int childTailPageNumber = dpMain.childTailPageNumber;
            if (dpMain.leaf) {
                if (childTailPageNumber != INVALID_INDEX_PAGE_NUMBER) {
                    throw new IllegalStateException(withErrorContext("Leaf page has tail " + dpMain));
                }
                return;
            }
            if (dpExtra.entryView.size() == 1 && dpMain.hasChildTail()) {
                throw new IllegalStateException(withErrorContext("Single child is tail " + dpMain));
            }
            Integer prevPageNumber = null;
            Integer nextPageNumber = null;
            Entry prevLastEntry = FIRST_ENTRY;
            for (Entry e : dpExtra.entryView) {
                validateEntryForPage(dpMain, e);
                Integer subPageNumber = e.getSubPageNumber();
                DataPageMain childMain = getPageForValidate(subPageNumber);
                if (childMain != null) {
                    if (prevPageNumber != null && !childMain.prevPageNumber.equals(prevPageNumber)) {
                        throw new IllegalStateException(withErrorContext("Child's prevPageNumber is not the previous child for " + childMain + " " + dpExtra.entryView + " " + prevPageNumber));
                    }
                    if (nextPageNumber != null && childMain.pageNumber != nextPageNumber) {
                        throw new IllegalStateException(withErrorContext("Child's pageNumber is not the expected next child for " + childMain));
                    }
                    if (childMain.parentPageNumber != null) {
                        if (childMain.parentPageNumber != dpMain.pageNumber) {
                            throw new IllegalStateException(withErrorContext("Child's parent is incorrect " + childMain));
                        }
                        boolean expectTail = subPageNumber == childTailPageNumber;
                        if (expectTail != childMain.tail) {
                            throw new IllegalStateException(withErrorContext("Child tail status incorrect " + childMain));
                        }
                    }
                    DataPageExtra childExtra = childMain.getExtra();
                    Entry lastEntry = childExtra.entryView.getLast();
                    if (e.compareTo(lastEntry) != 0) {
                        throw new IllegalStateException(withErrorContext("Invalid entry " + e + " but child is " + lastEntry));
                    }
                    Entry firstEntry = childExtra.entries.get(0);
                    if (prevLastEntry.compareTo(firstEntry) >= 0) {
                        throw new IllegalStateException(withErrorContext(
                            "Invalid first entry " + firstEntry + " but prev last is " + prevLastEntry));
                    }
                    nextPageNumber = childMain.nextPageNumber;
                    prevPageNumber = childMain.pageNumber;
                    prevLastEntry = lastEntry;
                } else {
                    // if we aren't force loading, we may have gaps in the children so we
                    // can't validate these for the current child
                    nextPageNumber = null;
                    prevPageNumber = null;
                }
            }
        }

        /**
         * Validates the peer pages for an index page.
         *
         * @param dpMain the index page
         */
        private void validatePeers(DataPageMain dpMain) throws IOException {

            DataPageMain prevMain = getPageForValidate(dpMain.prevPageNumber);
            if (prevMain != null) {
                if (prevMain.nextPageNumber != dpMain.pageNumber) {
                    throw new IllegalStateException(withErrorContext("Prev page " + prevMain + " does not ref " + dpMain));
                }
                validatePeerStatus(dpMain, prevMain);
                validatePeerEntries(prevMain, dpMain);
            }

            DataPageMain nextMain = getPageForValidate(dpMain.nextPageNumber);
            if (nextMain != null) {
                if (nextMain.prevPageNumber != dpMain.pageNumber) {
                    throw new IllegalStateException(withErrorContext("Next page " + nextMain + " does not ref " + dpMain));
                }
                validatePeerStatus(dpMain, nextMain);
                validatePeerEntries(dpMain, nextMain);
            }
        }

        /**
         * Validates the given peer page against the given index page
         *
         * @param dpMain the index page
         * @param peerMain the peer index page
         */
        private void validatePeerStatus(DataPageMain dpMain, DataPageMain peerMain) {
            if (dpMain.leaf != peerMain.leaf) {
                throw new IllegalStateException(withErrorContext("Mismatched peer status " + dpMain.leaf + " " + peerMain.leaf));
            }
            if (!dpMain.leaf) {
                if (dpMain.parentPageNumber != null && peerMain.parentPageNumber != null && !dpMain.parentPageNumber.equals(peerMain.parentPageNumber)) {
                    throw new IllegalStateException(withErrorContext("Mismatched node parents " + dpMain.parentPageNumber + " " + peerMain.parentPageNumber));
                }
            }
        }

        /**
         * Validates the order of the entries of the peers.
         */
        private void validatePeerEntries(DataPageMain prevMain, DataPageMain nextMain) throws IOException {
            Entry lastE = prevMain.getExtra().entryView.getLast();
            Entry firstE = nextMain.getExtra().entries.get(0);
            if (lastE.compareTo(firstE) >= 0) {
                throw new IOException(
                    withErrorContext("Unexpected peer order in index entries, " + lastE + " >= " + firstE));
            }
        }

        private DataPageMain getPageForValidate(Integer pageNumber) throws IOException {
            DataPageMain dpMain = knownPages.get(pageNumber);
            if (dpMain == null && forceLoad && pageNumber != INVALID_INDEX_PAGE_NUMBER) {
                dpMain = getDataPage(pageNumber);
                if (dpMain != null) {
                    knownPages.put(pageNumber, dpMain);
                    pendingPages.add(dpMain);
                } else {
                    throw new IllegalStateException(withErrorContext("Could not find index page " + pageNumber));
                }
            }
            return dpMain;
        }
    }

}
