/*
 * (c) Kitodo. Key to digital objects e. V. <contact@kitodo.org>
 *
 * This file is part of the Kitodo project.
 *
 * It is licensed under GNU General Public License version 3 or later.
 *
 * For the full copyright and license information, please read the
 * GPL3-License.txt file that was distributed with this source code.
 */

package org.kitodo.forms;

import java.io.Serializable;
import java.util.List;

import org.kitodo.data.database.beans.User;
import org.kitodo.model.LazyDTOModel;
import org.kitodo.services.ServiceManager;
import org.primefaces.event.TabChangeEvent;

public class BaseForm implements Serializable {

    private static final long serialVersionUID = 2950419497162710096L;
    protected transient ServiceManager serviceManager = new ServiceManager();
    String zurueck = null;
    protected String filter = "";
    protected User user;
    protected LazyDTOModel lazyDTOModel = null;
    static final String REDIRECT_PARAMETER = "faces-redirect=true";
    private static final String TEMPLATE_ROOT = "/pages/";
    private int activeTabId = 0;
    private boolean saveDisabled = true;
    private boolean listLiveScroll = true;

    // error messages
    protected static final String ERROR_CREATING = "errorCreating";
    protected static final String ERROR_DATABASE_READING = "errorDatabaseReading";
    protected static final String ERROR_DUPLICATE = "errorDuplicate";
    protected static final String ERROR_EXPORTING = "errorExporting";
    protected static final String ERROR_LOADING_MANY = "errorLoadingMany";
    protected static final String ERROR_LOADING_ONE = "errorLoadingOne";
    protected static final String ERROR_DELETING = "errorDeleting";
    protected static final String ERROR_READING = "errorReading";
    protected static final String ERROR_RELOADING = "errorReloading";
    protected static final String ERROR_SAVING = "errorSaving";

    protected static final String REDIRECT_PATH = TEMPLATE_ROOT + "{0}?" + REDIRECT_PARAMETER;
    protected static final String DEFAULT_LINK = "desktop";

    /**
     * Getter: return lazyDTOModel.
     *
     * @return LazyDTOModel
     */
    public LazyDTOModel getLazyDTOModel() {
        return lazyDTOModel;
    }

    /**
     * Setter: set lazyDTOModel.
     *
     * @param lazyDTOModel
     *            LazyDTOModel to set for this class
     */
    public void setLazyDTOModel(LazyDTOModel lazyDTOModel) {
        this.lazyDTOModel = lazyDTOModel;
    }

    public String getZurueck() {
        return this.zurueck;
    }

    public void setZurueck(String zurueck) {
        this.zurueck = zurueck;
    }

    /**
     * Get User.
     *
     * @return User
     */
    public User getUser() {
        if (this.user == null) {
            this.user = serviceManager.getUserService().getAuthenticatedUser();
        }
        return this.user;
    }

    public String getFilter() {
        return this.filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    /**
     * Add filter to user.
     */
    public void addFilterToUser() {
        if (this.filter == null || this.filter.length() == 0) {
            return;
        }
        serviceManager.getUserService().addFilter(getUser(), this.filter);
    }

    /**
     * Get user filters.
     */
    public List<String> getUserFilters() {
        return serviceManager.getUserService().getFilters(getUser());
    }

    /**
     * Remove filter from user.
     */
    public void removeFilterFromUser() {
        if (this.filter == null || this.filter.length() == 0) {
            return;
        }
        serviceManager.getUserService().removeFilter(getUser(), this.filter);
    }

    /**
     * Return index of active tab.
     *
     * @return index of active tab
     */
    public int getActiveTabIndex() {
        return activeTabId;
    }

    /**
     * Set index of active tab.
     *
     * @param id
     *            index of active tab
     */
    public void setActiveTabIndex(int id) {
        this.activeTabId = id;
    }

    /**
     * Updates the active tab index whenever the TabChangeEvent is fired.
     *
     * @param event
     *            TabChangeEvent is fired when the user changes the tab in the
     *            current tab view
     */
    public void onTabChange(TabChangeEvent event) {
        setActiveTabIndex(event.getComponent().getChildren().indexOf(event.getTab()));
    }

    /**
     * Return boolean to disable save button.
     *
     * @return status of save button
     */
    public boolean isSaveDisabled() {
        return saveDisabled;
    }

    /**
     * Set boolean to disable save button.
     *
     * @param saveDisabled true or false
     */
    public void setSaveDisabled(boolean saveDisabled) {
        this.saveDisabled = saveDisabled;
    }

    /**
     * Get boolean flag controlling the live list scrolling of data tables.
     *
     * @return flag indicating whether live list scrolling is active or not.
     */
    public boolean getListLiveScroll() {
        return this.listLiveScroll;
    }

    /**
     * Set flag controlling the live list scrolling of data tables.
     *
     * @param listLiveScroll true or false
     */
    public void setListLiveScroll(boolean listLiveScroll) {
        this.listLiveScroll = listLiveScroll;
    }
}
