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

package de.sub.goobi.forms;

import de.sub.goobi.config.ConfigCore;

import java.io.FilenameFilter;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.SessionScoped;
import javax.faces.model.SelectItem;
import javax.inject.Named;

import org.goobi.production.GoobiVersion;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.PluginLoader;
import org.kitodo.api.filemanagement.filters.FileNameEndsWithFilter;
import org.kitodo.data.database.beans.Docket;
import org.kitodo.data.database.beans.Ruleset;
import org.kitodo.data.database.helper.enums.MetadataFormat;
import org.kitodo.services.ServiceManager;

/**
 * Helper form.
 *
 * @author Wulf Riebensahm
 */
@Named("HelperForm")
@SessionScoped
public class HelperForm implements Serializable {
    private static final long serialVersionUID = -5872893771807845586L;
    private transient ServiceManager serviceManager = new ServiceManager();
    private static final String CSS_BASE_PATH = "/WEB-INF/resources/css";
    private static final String CSS_PATH = "/userStyles";

    public String getVersion() {
        return GoobiVersion.getBuildversion();
    }

    public boolean getAnonymized() {
        return ConfigCore.getBooleanParameter("anonymize");
    }

    /**
     * Get rulesets.
     *
     * @return list of rulesets as SelectItems
     */
    public List<SelectItem> getRulesets() {
        List<SelectItem> myPrefs = new ArrayList<>();
        List<Ruleset> temp = serviceManager.getRulesetService().getByQuery("from Ruleset ORDER BY title");
        for (Ruleset ruleset : temp) {
            myPrefs.add(new SelectItem(ruleset.getId(), ruleset.getTitle(), null));
        }
        return myPrefs;
    }

    /**
     * Get dockets.
     *
     * @return list of dockets as SelectItems
     */
    public List<SelectItem> getDockets() {
        List<SelectItem> answer = new ArrayList<>();
        List<Docket> temp = serviceManager.getDocketService().getByQuery("from Docket ORDER BY title");
        for (Docket d : temp) {
            answer.add(new SelectItem(d.getId(), d.getTitle(), null));
        }
        return answer;
    }

    /**
     * Get file formats.
     *
     * @return list of file formats as Strings
     */
    public List<String> getFileFormats() {
        ArrayList<String> ffs = new ArrayList<>();
        for (MetadataFormat ffh : MetadataFormat.values()) {
            if (!ffh.equals(MetadataFormat.RDF)) {
                ffs.add(ffh.getName());
            }
        }
        return ffs;
    }

    /**
     * Get only internal file formats.
     *
     * @return list of internal file formats as Strings
     */
    public List<String> getFileFormatsInternalOnly() {
        ArrayList<String> ffs = new ArrayList<>();
        for (MetadataFormat ffh : MetadataFormat.values()) {
            if (ffh.isUsableForInternal() && !ffh.equals(MetadataFormat.RDF)) {
                ffs.add(ffh.getName());
            }
        }
        return ffs;
    }

    /**
     * Get all css files from root folder.
     *
     * @return list of css files
     */
    public List<SelectItem> getCssFiles() {
        List<SelectItem> list = new ArrayList<>();
        FilenameFilter filter = new FileNameEndsWithFilter(".css");
        List<URI> uris = serviceManager.getFileService().getSubUris(filter, URI.create(CSS_BASE_PATH + CSS_PATH));
        for (URI uri : uris) {
            list.add(new SelectItem(uri.toString(), uri.toString()));
        }
        return list;
    }

    /**
     * Check if mass import is allowed.
     *
     * @return true or false
     */
    public boolean getMassImportAllowed() {
        return ConfigCore.getBooleanParameter("massImportAllowed", false)
                && !PluginLoader.getPluginList(PluginType.IMPORT).isEmpty();
    }

    /**
     * Returning value of configuration parameter withUserStepDoneSearch. Used
     * for enabling/disabling search for done steps by user.
     *
     * @return boolean
     */
    public boolean getUserStepDoneSearchEnabled() {
        return ConfigCore.getBooleanParameter("withUserStepDoneSearch");
    }
}
