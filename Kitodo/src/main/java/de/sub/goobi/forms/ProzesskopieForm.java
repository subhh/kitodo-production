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
import de.sub.goobi.config.ConfigProjects;
import de.sub.goobi.config.DigitalCollections;
import de.sub.goobi.helper.BeanHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.UghHelper;
import de.sub.goobi.helper.exceptions.UghHelperException;
import de.sub.goobi.metadaten.copier.CopierData;
import de.sub.goobi.metadaten.copier.DataCopier;
import de.unigoettingen.sub.search.opac.ConfigOpac;
import de.unigoettingen.sub.search.opac.ConfigOpacDoctype;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javax.enterprise.context.SessionScoped;
import javax.faces.model.SelectItem;
import javax.inject.Named;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.goobi.production.cli.helper.WikiFieldHelper;
import org.goobi.production.constants.Parameters;
import org.goobi.production.plugin.CataloguePlugin.CataloguePlugin;
import org.goobi.production.plugin.CataloguePlugin.Hit;
import org.goobi.production.plugin.CataloguePlugin.QueryBuilder;
import org.goobi.production.plugin.PluginLoader;
import org.jdom.JDOMException;
import org.kitodo.api.ugh.DigitalDocumentInterface;
import org.kitodo.api.ugh.DocStructInterface;
import org.kitodo.api.ugh.DocStructTypeInterface;
import org.kitodo.api.ugh.FileformatInterface;
import org.kitodo.api.ugh.MetadataInterface;
import org.kitodo.api.ugh.MetadataTypeInterface;
import org.kitodo.api.ugh.PersonInterface;
import org.kitodo.api.ugh.PrefsInterface;
import org.kitodo.api.ugh.exceptions.DocStructHasNoTypeException;
import org.kitodo.api.ugh.exceptions.MetadataTypeNotAllowedException;
import org.kitodo.api.ugh.exceptions.PreferencesException;
import org.kitodo.api.ugh.exceptions.ReadException;
import org.kitodo.api.ugh.exceptions.TypeNotAllowedAsChildException;
import org.kitodo.api.ugh.exceptions.UGHException;
import org.kitodo.api.ugh.exceptions.WriteException;
import org.kitodo.data.database.beans.BaseTemplateBean;
import org.kitodo.data.database.beans.Process;
import org.kitodo.data.database.beans.Project;
import org.kitodo.data.database.beans.Property;
import org.kitodo.data.database.beans.Task;
import org.kitodo.data.database.beans.Template;
import org.kitodo.data.database.beans.User;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.database.helper.enums.TaskEditType;
import org.kitodo.data.database.helper.enums.TaskStatus;
import org.kitodo.data.exceptions.DataException;
import org.kitodo.exceptions.ProcessCreationException;
import org.kitodo.legacy.UghImplementation;
import org.kitodo.production.thread.TaskScriptThread;
import org.kitodo.services.ServiceManager;
import org.omnifaces.util.Ajax;
import org.primefaces.context.RequestContext;

@Named("ProzesskopieForm")
@SessionScoped
public class ProzesskopieForm implements Serializable {
    private static final Logger logger = LogManager.getLogger(ProzesskopieForm.class);
    private static final long serialVersionUID = -4512865679353743L;
    protected static final String CREATE_NEW_PROCESS = "createNewProcess";
    protected static final String ERROR_READ = "errorReading";
    protected static final String ITEM_LIST = CREATE_NEW_PROCESS + ".itemlist";
    protected static final String ITEM_LIST_ITEM = ITEM_LIST + ".item";
    private static final String ITEM_LIST_PROCESS_TITLE = ITEM_LIST + ".processtitle";
    private static final String OPAC_CONFIG = "configurationOPAC";
    private static final String BOUND_BOOK = "boundbook";
    private static final String FIRST_CHILD = "firstchild";
    private static final String LIST_OF_CREATORS = "ListOfCreators";
    private transient ServiceManager serviceManager = new ServiceManager();

    private int activeTabId = 0;

    /**
     * Get activeTabId.
     *
     * @return value of activeTabId
     */
    public int getActiveTabId() {
        return activeTabId;
    }

    /**
     * Set activeTabId.
     *
     * @param activeTabId
     *            as int
     */
    public void setActiveTabId(int activeTabId) {
        this.activeTabId = activeTabId;
    }

    /**
     * The class SelectableHit represents a hit on the hit list that shows up if
     * a catalogue search yielded more than one result. We need an inner class
     * for this because Faces is strictly object oriented and the always
     * argument-less actions can only be executed relatively to the list entry
     * in question this way if they are concerning elements that are rendered by
     * iterating along a list.
     *
     * @author Matthias Ronge &lt;matthias.ronge@zeutschel.de&gt;
     */
    public class SelectableHit {
        /**
         * The field hit holds the hit to be rendered as a list entry.
         */
        private final Hit hit;

        /**
         * The field error holds an error message to be rendered as a list entry
         * in case that retrieving the hit failed within the plug-in used for
         * catalogue access.
         */
        private final String error;

        /**
         * Selectable hit constructor. Creates a new SelectableHit object with a
         * hit to show.
         *
         * @param hit
         *            Hit to show
         */
        public SelectableHit(Hit hit) {
            this.hit = hit;
            error = null;
        }

        /**
         * Selectable hit constructor. Creates a new SelectableHit object with
         * an error message to show.
         *
         * @param error
         *            error message
         */
        public SelectableHit(String error) {
            hit = null;
            this.error = error;
        }

        /**
         * The function getBibliographicCitation() returns a summary of this hit
         * in bibliographic citation style as HTML as read-only property
         * “bibliographicCitation”.
         *
         * @return a summary of this hit in bibliographic citation style as HTML
         */
        public String getBibliographicCitation() {
            return hit.getBibliographicCitation();
        }

        /**
         * The function getErrorMessage() returns an error if that had occurred
         * when trying to retrieve that hit from the catalogue as read-only
         * property “errorMessage”.
         *
         * @return an error message to be rendered as a list entry
         */
        public String getErrorMessage() {
            return error;
        }

        /**
         * The function isError() returns whether an error occurred when trying
         * to retrieve that hit from the catalogue as read-only property
         * “error”.
         *
         * @return whether an error occurred when retrieving that hit
         */
        public boolean isError() {
            return hit == null;
        }

        /**
         * The function selectClick() is called if the user clicks on a
         * catalogue hit summary in order to import it into Production.
         */
        public void selectClick() {
            try {
                importHit(hit);
            } catch (PreferencesException | RuntimeException e) {
                Helper.setErrorMessage(ERROR_READ, new Object[] {"OPAC" }, logger, e);
            } finally {
                hitlistPage = -1;
            }
        }
    }

    /**
     * The constant DEFAULT_HITLIST_PAGE_SIZE holds the fallback number of hits
     * to show per page on the hit list if the user conducted a catalogue search
     * that yielded more than one result, if none is configured in the
     * Production configuration file.
     */
    private static final int DEFAULT_HITLIST_PAGE_SIZE = 10;
    private static final String DIRECTORY_SUFFIX = "_tif";
    private String addToWikiField = "";
    private List<AdditionalField> additionalFields;
    private String atstsl = "";
    private List<String> digitalCollections;
    private String docType;
    private Integer guessedImages = 0;

    /**
     * The field hitlist holds some reference to the hitlist retrieved from a
     * library catalogue. The internals of this object are subject to the plugin
     * implementation and are not to be accessed directly.
     */
    private Object hitlist;

    /**
     * The field hitlistPage holds the zero-based index of the page of the
     * hitlist currently showing. A negative value means that the hitlist is
     * hidden, otherwise it is showing the respective page.
     */
    private long hitlistPage = -1;
    /**
     * The field hits holds the number of hits in the hitlist last retrieved
     * from a library catalogue.
     */
    private long hits;

    /**
     * The field importCatalogue holds the catalogue plugin used to access the
     * library catalogue.
     */
    private CataloguePlugin importCatalogue;

    private FileformatInterface rdf;
    private String opacSuchfeld = "12";
    private String opacSuchbegriff;
    private String opacKatalog;
    private List<String> possibleDigitalCollection;
    private Template template = new Template();
    private Process prozessKopie = new Process();
    private boolean useOpac;
    private boolean useTemplates;
    private Integer auswahl;
    private HashMap<String, Boolean> standardFields;
    private String tifHeaderImageDescription = "";
    private String tifHeaderDocumentName = "";
    private List<String> workflowConditions = new ArrayList<>();
    private static final String REDIRECT_PATH = "/pages/{0}?" + "faces-redirect=true";

    private String processListPath = MessageFormat.format(REDIRECT_PATH, "processes");
    private String processFromTemplatePath = MessageFormat.format(REDIRECT_PATH, "processFromTemplate");

    protected static final String INCOMPLETE_DATA = "errorDataIncomplete";

    /**
     * Prepare.
     *
     * @return empty String
     */
    public String prepare(int id) {
        atstsl = "";
        try {
            this.template = serviceManager.getTemplateService().getById(id);
        } catch (DAOException e) {
            logger.error(e.getMessage());
            Helper.setErrorMessage("Process " + id + " not found.");
            return null;
        }

        if (serviceManager.getTemplateService().containsBeanUnreachableSteps(this.template.getTasks())) {
            if (this.template.getTasks().isEmpty()) {
                Helper.setErrorMessage("noStepsInWorkflow");
            }
            for (Task s : this.template.getTasks()) {
                if (serviceManager.getTaskService().getUserGroupsSize(s) == 0
                        && serviceManager.getTaskService().getUsersSize(s) == 0) {
                    Helper.setErrorMessage("noUserInStep", new Object[] {s.getTitle() });
                }
            }
            return null;
        }

        clearValues();
        readProjectConfigs();
        this.rdf = null;
        this.prozessKopie = new Process();
        this.prozessKopie.setTitle("");
        this.prozessKopie.setProject(this.template.getProject());
        this.prozessKopie.setRuleset(this.template.getRuleset());
        this.prozessKopie.setDocket(this.template.getDocket());
        this.digitalCollections = new ArrayList<>();

        BeanHelper.copyTasks(this.template, this.prozessKopie);

        initializePossibleDigitalCollections();

        return processFromTemplatePath;
    }

    private void readProjectConfigs() {
        // projektabhängig die richtigen Felder in der Gui anzeigen
        ConfigProjects cp;
        try {
            cp = new ConfigProjects(this.template.getProject().getTitle());
        } catch (IOException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
            return;
        }

        this.docType = cp.getParamString(CREATE_NEW_PROCESS + ".defaultdoctype",
            ConfigOpac.getAllDoctypes().get(0).getTitle());
        this.useOpac = cp.getParamBoolean(CREATE_NEW_PROCESS + ".opac[@use]");
        this.useTemplates = cp.getParamBoolean(CREATE_NEW_PROCESS + ".templates[@use]");
        if (this.opacKatalog.equals("")) {
            this.opacKatalog = cp.getParamString(CREATE_NEW_PROCESS + ".opac.catalogue");
        }

        // die auszublendenden Standard-Felder ermitteln
        for (String t : cp.getParamList(ITEM_LIST + ".hide")) {
            this.standardFields.put(t, false);
        }

        // die einzublendenen (zusätzlichen) Eigenschaften ermitteln
        int count = cp.getParamList(ITEM_LIST_ITEM).size();
        for (int i = 0; i < count; i++) {
            AdditionalField fa = new AdditionalField(this);
            fa.setFrom(cp.getParamString(ITEM_LIST_ITEM + "(" + i + ")[@from]"));
            fa.setTitle(cp.getParamString(ITEM_LIST_ITEM + "(" + i + ")"));
            fa.setRequired(cp.getParamBoolean(ITEM_LIST_ITEM + "(" + i + ")[@required]"));
            fa.setIsdoctype(cp.getParamString(ITEM_LIST_ITEM + "(" + i + ")[@isdoctype]"));
            fa.setIsnotdoctype(cp.getParamString(ITEM_LIST_ITEM + "(" + i + ")[@isnotdoctype]"));
            // attributes added 30.3.09
            String test = (cp.getParamString(ITEM_LIST_ITEM + "(" + i + ")[@initStart]"));
            fa.setInitStart(test);

            fa.setInitEnd(cp.getParamString(ITEM_LIST_ITEM + "(" + i + ")[@initEnd]"));

            /*
             * Bindung an ein Metadatum eines Docstructs
             */
            if (cp.getParamBoolean(ITEM_LIST_ITEM + "(" + i + ")[@ughbinding]")) {
                fa.setUghbinding(true);
                fa.setDocstruct(cp.getParamString(ITEM_LIST_ITEM + "(" + i + ")[@docstruct]"));
                fa.setMetadata(cp.getParamString(ITEM_LIST_ITEM + "(" + i + ")[@metadata]"));
            }
            if (cp.getParamBoolean(ITEM_LIST_ITEM + "(" + i + ")[@autogenerated]")) {
                fa.setAutogenerated(true);
            }

            /*
             * prüfen, ob das aktuelle Item eine Auswahlliste werden soll
             */
            int selectItemCount = cp.getParamList(ITEM_LIST_ITEM + "(" + i + ").select").size();
            /* Children durchlaufen und SelectItems erzeugen */
            if (selectItemCount > 0) {
                fa.setSelectList(new ArrayList<>());
            }
            for (int j = 0; j < selectItemCount; j++) {
                String svalue = cp.getParamString(ITEM_LIST_ITEM + "(" + i + ").select(" + j + ")[@label]");
                String sid = cp.getParamString(ITEM_LIST_ITEM + "(" + i + ").select(" + j + ")");
                fa.getSelectList().add(new SelectItem(sid, svalue, null));
            }
            this.additionalFields.add(fa);
        }
    }

    /**
     * Get Process templates.
     *
     * @return list of SelectItem objects
     */
    public List<SelectItem> getProcessesForChoiceList() {
        List<Process> processes = new ArrayList<>();
        // TODO Change to check the corresponding authority
        if (serviceManager.getSecurityAccessService().isAdmin()) {
            try {
                processes = serviceManager.getProcessService().getAll();
            } catch (DAOException e) {
                logger.error(e.getMessage(), e);
            }
        } else {
            User currentUser = serviceManager.getUserService().getAuthenticatedUser();
            if (Objects.nonNull(currentUser)) {
                for (Project project : currentUser.getProjects()) {
                    processes.addAll(project.getProcesses());
                }
            }
        }

        processes = processes.stream().filter(BaseTemplateBean::getInChoiceListShown).collect(Collectors.toList());

        List<SelectItem> processSelectItems = new ArrayList<>();
        for (Process process : processes) {
            processSelectItems.add(new SelectItem(process.getId(), process.getTitle(), null));
        }
        return processSelectItems;
    }

    /**
     * The function evaluateOpac() is executed if a user clicks the command link
     * to start a catalogue search. It performs the search and loads the hit if
     * it is unique. Otherwise, it will cause a hit list to show up for the user
     * to select a hit.
     */
    public void evaluateOpac() {
        long timeout = CataloguePlugin.getTimeout();
        clearValues();
        RequestContext.getCurrentInstance().update("hitlistForm");
        try {
            readProjectConfigs();
            if (pluginAvailableFor(opacKatalog)) {
                String query = QueryBuilder.restrictToField(opacSuchfeld, opacSuchbegriff);
                query = QueryBuilder.appendAll(query, ConfigOpac.getRestrictionsForCatalogue(opacKatalog));

                hitlist = importCatalogue.find(query, timeout);
                hits = importCatalogue.getNumberOfHits(hitlist, timeout);

                String message = MessageFormat.format(Helper.getTranslation("newProcess.catalogueSearch.results"),
                    hits);

                switch ((int) Math.min(hits, Integer.MAX_VALUE)) {
                    case 0:
                        Helper.setErrorMessage(message);
                        break;
                    case 1:
                        importHit(importCatalogue.getHit(hitlist, 0, timeout));
                        Helper.setMessage(message);
                        break;
                    default:
                        hitlistPage = 0; // show first page of hitlist
                        Helper.setMessage(message);
                        RequestContext.getCurrentInstance().execute("PF('hitlistDialog').show()");
                        break;
                }
            } else {
                Helper.setErrorMessage("ERROR: No suitable plugin available for OPAC '" + opacKatalog + "'");
            }
        } catch (FileNotFoundException | PreferencesException | RuntimeException e) {
            Helper.setErrorMessage(ERROR_READ, new Object[] {"OPAC " + opacKatalog }, logger, e);
        }
    }

    /**
     * The function pluginAvailableFor(catalogue) verifies that a plugin
     * suitable for accessing the library catalogue identified by the given
     * String is available in the global variable importCatalogue. If
     * importCatalogue is empty or the current plugin doesn’t support the given
     * catalogue, the function will try to load a suitable plugin. Upon success
     * the preferences and the catalogue to use will be configured in the
     * plugin, otherwise an error message will be set to be shown.
     *
     * @param catalogue
     *            identifier string for the catalogue that the plugin shall support
     * @return whether a plugin is available in the global variable importCatalogue
     */
    private boolean pluginAvailableFor(String catalogue) {
        if (importCatalogue == null || !importCatalogue.supportsCatalogue(catalogue)) {
            importCatalogue = PluginLoader.getCataloguePluginForCatalogue(catalogue);
        }
        if (importCatalogue == null) {
            Helper.setErrorMessage("NoCataloguePluginForCatalogue", catalogue);
            return false;
        } else {
            importCatalogue
                    .setPreferences(serviceManager.getRulesetService().getPreferences(prozessKopie.getRuleset()));
            importCatalogue.useCatalogue(catalogue);
            return true;
        }
    }

    /**
     * alle Konfigurationseigenschaften und Felder zurücksetzen.
     */
    private void clearValues() {
        if (this.opacKatalog == null) {
            this.opacKatalog = "";
        }
        this.standardFields = new HashMap<>();
        this.standardFields.put("collections", true);
        this.standardFields.put("doctype", true);
        this.standardFields.put("regelsatz", true);
        this.standardFields.put("images", true);
        this.additionalFields = new ArrayList<>();
        this.tifHeaderDocumentName = "";
        this.tifHeaderImageDescription = "";
    }

    /**
     * The method importHit() loads a hit into the display.
     *
     * @param hit
     *            Hit to load
     */
    protected void importHit(Hit hit) throws PreferencesException {
        rdf = hit.getFileformat();
        docType = hit.getDocType();
        fillFieldsFromMetadataFile();
        applyCopyingRules(new CopierData(rdf, this.template));
        atstsl = createAtstsl(hit.getTitle(), hit.getAuthors());
        setActiveTabId(0);
    }

    /**
     * Creates a DataCopier with the given configuration, lets it process the
     * given data and wraps any errors to display in the front end.
     *
     * @param data
     *            data to process
     */
    private void applyCopyingRules(CopierData data) {
        String rules = ConfigCore.getParameter("copyData.onCatalogueQuery");
        if (Objects.nonNull(rules)) {
            try {
                new DataCopier(rules).process(data);
            } catch (ConfigurationException e) {
                Helper.setErrorMessage("dataCopier.syntaxError", logger, e);
            } catch (RuntimeException e) {
                if (RuntimeException.class.equals(e.getClass())) {
                    Helper.setErrorMessage("dataCopier.runtimeException", logger, e);
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * die Eingabefelder für die Eigenschaften mit Inhalten aus der RDF-Datei
     * füllen.
     */
    private void fillFieldsFromMetadataFile() throws PreferencesException {
        if (this.rdf != null) {
            for (AdditionalField field : this.additionalFields) {
                if (field.isUghbinding() && field.getShowDependingOnDoctype()) {
                    proceedField(field);
                    if (field.getValue() != null && !field.getValue().equals("")) {
                        field.setValue(field.getValue().replace("&amp;", "&"));
                    }
                }
            }
        }
    }

    private void proceedField(AdditionalField field) throws PreferencesException {
        DocStructInterface docStruct = getDocStruct(field);
        try {
            if (field.getMetadata().equals(LIST_OF_CREATORS)) {
                field.setValue(getAuthors(docStruct.getAllPersons()));
            } else {
                // evaluate the content in normal fields
                MetadataTypeInterface mdt = UghHelper.getMetadataType(
                    serviceManager.getRulesetService().getPreferences(this.prozessKopie.getRuleset()),
                    field.getMetadata());
                MetadataInterface md = UghHelper.getMetadata(docStruct, mdt);
                if (md != null) {
                    field.setValue(md.getValue());
                    md.setStringValue(field.getValue().replace("&amp;", "&"));
                }
            }
        } catch (UghHelperException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
    }

    private DocStructInterface getDocStruct(AdditionalField field) throws PreferencesException {
        DigitalDocumentInterface digitalDocument = this.rdf.getDigitalDocument();
        DocStructInterface docStruct = digitalDocument.getLogicalDocStruct();
        if (field.getDocstruct().equals(FIRST_CHILD)) {
            docStruct = digitalDocument.getLogicalDocStruct().getAllChildren().get(0);
        }
        if (field.getDocstruct().equals(BOUND_BOOK)) {
            docStruct = digitalDocument.getPhysicalDocStruct();
        }
        return docStruct;
    }

    /**
     * Get together authors' names.
     *
     * @param persons
     *            list of persons
     * @return authors' names as String
     */
    protected String getAuthors(List<PersonInterface> persons) {
        StringBuilder authors = new StringBuilder();

        if (persons != null) {
            for (PersonInterface p : persons) {
                authors.append(p.getLastName());
                if (StringUtils.isNotBlank(p.getFirstName())) {
                    authors.append(", ");
                    authors.append(p.getFirstName());
                }
                authors.append("; ");
            }
            if (authors.toString().endsWith("; ")) {
                authors.setLength(authors.length() - 2);
            }
        }

        return authors.toString();
    }

    /**
     * Auswahl des Prozesses auswerten.
     */
    public String templateAuswahlAuswerten() throws DAOException {
        /* den ausgewählten Prozess laden */
        Process tempProzess = serviceManager.getProcessService().getById(this.auswahl);
        if (serviceManager.getProcessService().getWorkpiecesSize(tempProzess) > 0) {
            for (Property workpieceProperty : tempProzess.getWorkpieces()) {
                for (AdditionalField field : this.additionalFields) {
                    if (field.getTitle().equals(workpieceProperty.getTitle())) {
                        field.setValue(workpieceProperty.getValue());
                    }
                    if (workpieceProperty.getTitle().equals("DocType")) {
                        docType = workpieceProperty.getValue();
                    }
                }
            }
        }

        if (serviceManager.getProcessService().getTemplatesSize(tempProzess) > 0) {
            for (Property templateProperty : tempProzess.getTemplates()) {
                for (AdditionalField field : this.additionalFields) {
                    if (field.getTitle().equals(templateProperty.getTitle())) {
                        field.setValue(templateProperty.getValue());
                    }
                }
            }
        }

        if (serviceManager.getProcessService().getPropertiesSize(tempProzess) > 0) {
            for (Property processProperty : tempProzess.getProperties()) {
                if (processProperty.getTitle().equals("digitalCollection")) {
                    digitalCollections.add(processProperty.getValue());
                }
            }
        }

        try {
            this.rdf = serviceManager.getProcessService().readMetadataAsTemplateFile(tempProzess);
        } catch (ReadException | PreferencesException | IOException | RuntimeException e) {
            Helper.setErrorMessage(ERROR_READ, new Object[] {"template-metadata" }, logger, e);
        }

        removeCollectionsForChildren();
        return null;
    }

    /**
     * If there is a first child, the collections are for it.
     */
    private void removeCollectionsForChildren() {
        try {
            DocStructInterface colStruct = this.rdf.getDigitalDocument().getLogicalDocStruct();
            removeCollections(colStruct, this.prozessKopie);
            colStruct = colStruct.getAllChildren().get(0);
            removeCollections(colStruct, this.prozessKopie);
        } catch (PreferencesException e) {
            Helper.setErrorMessage("Error on creating process", logger, e);
        } catch (RuntimeException e) {
            logger.debug("das Firstchild unterhalb des Topstructs konnte nicht ermittelt werden", e);
        }
    }

    /**
     * Validierung der Eingaben.
     *
     * @return sind Fehler bei den Eingaben vorhanden?
     */
    boolean isContentValid() {
        return isContentValid(true);
    }

    boolean isContentValid(boolean criticiseEmptyTitle) {
        boolean valid = true;

        if (criticiseEmptyTitle) {
            valid = isProcessTitleCorrect(this.prozessKopie);
        }

        /*
         * Prüfung der standard-Eingaben, die angegeben werden müssen
         */
        /* keine Collektion ausgewählt */
        if (this.standardFields.get("collections") && getDigitalCollections().isEmpty()) {
            valid = false;
            Helper.setErrorMessage(INCOMPLETE_DATA, "processCreationErrorNoCollection");
        }

        /*
         * Prüfung der additional-Eingaben, die angegeben werden müssen
         */
        for (AdditionalField field : this.additionalFields) {
            if ((field.getValue() == null || field.getValue().equals("")) && field.isRequired()
                    && field.getShowDependingOnDoctype() && (StringUtils.isBlank(field.getValue()))) {
                valid = false;
                Helper.setErrorMessage(INCOMPLETE_DATA,
                    " " + field.getTitle() + " " + Helper.getTranslation("processCreationErrorFieldIsEmpty"));

            }
        }
        return valid;
    }

    protected boolean isProcessTitleCorrect(Process process) {
        boolean valid = true;

        if (process.getTitle() == null || process.getTitle().equals("")) {
            valid = false;
            Helper.setErrorMessage(INCOMPLETE_DATA, "processCreationErrorTitleEmpty");
        }

        String validateRegEx = ConfigCore.getParameter("validateProzessTitelRegex", "[\\w-]*");
        if (!process.getTitle().matches(validateRegEx)) {
            valid = false;
            Helper.setErrorMessage("processTitleInvalid");
        }

        if (process.getTitle() != null) {
            valid = isProcessTitleAvailable(process.getTitle());
        }
        return valid;
    }

    /**
     * Checks if process title is available. If yes, return true, if no, return
     * false.
     *
     * @param title
     *            of process
     * @return boolean
     */
    protected boolean isProcessTitleAvailable(String title) {
        long amount;
        try {
            amount = serviceManager.getProcessService().findNumberOfProcessesWithTitle(title);
        } catch (DataException e) {
            Helper.setErrorMessage(ERROR_READ, new Object[] {Helper.getTranslation("process") }, logger, e);
            return false;
        }
        if (amount > 0) {
            Helper.setErrorMessage(Helper.getTranslation(INCOMPLETE_DATA)
                    + Helper.getTranslation("processCreationErrorTitleAlreadyInUse"));
            return false;
        }
        return true;
    }

    /**
     * Anlegen des Prozesses und save der Metadaten.
     */
    public String createNewProcess() throws ReadException, IOException, PreferencesException, WriteException {

        // evict set up id to null
        serviceManager.getProcessService().evict(this.prozessKopie);
        if (!isContentValid()) {
            return null;
        }
        addProperties();

        updateTasks(this.prozessKopie);

        try {
            this.prozessKopie.setSortHelperImages(this.guessedImages);
            serviceManager.getProcessService().save(this.prozessKopie);
            serviceManager.getProcessService().refresh(this.prozessKopie);
        } catch (DataException e) {
            Helper.setErrorMessage("errorCreating", new Object[] {Helper.getTranslation("process") }, logger, e);
            return null;
        }

        String baseProcessDirectory = serviceManager.getProcessService().getProcessDataDirectory(this.prozessKopie)
                .toString();
        boolean successful = serviceManager.getFileService().createMetaDirectory(URI.create(""), baseProcessDirectory);
        if (!successful) {
            String message = "Metadata directory: " + baseProcessDirectory + "in path:"
                    + ConfigCore.getKitodoDataDirectory() + " was not created!";
            logger.error(message);
            Helper.setErrorMessage(message);
            return null;
        }

        /*
         * wenn noch keine RDF-Datei vorhanden ist (weil keine Opac-Abfrage
         * stattfand, dann jetzt eine anlegen
         */
        if (this.rdf == null) {
            createNewFileformat();
        }

        /*
         * wenn eine RDF-Konfiguration vorhanden ist (z.B. aus dem Opac-Import,
         * oder frisch angelegt), dann diese ergänzen
         */
        if (this.rdf != null) {
            insertLogicalDocStruct();

            for (AdditionalField field : this.additionalFields) {
                if (field.isUghbinding() && field.getShowDependingOnDoctype()) {
                    processAdditionalField(field);
                }
            }

            updateMetadata();
            insertCollections();
            insertImagePath();
        }

        // Create configured directories
        serviceManager.getProcessService().createProcessDirs(this.prozessKopie);
        serviceManager.getProcessService().readMetadataFile(this.prozessKopie);

        startTaskScriptThreads();

        return processListPath;
    }

    private void processAdditionalField(AdditionalField field) throws PreferencesException {
        // which DocStruct
        DocStructInterface tempStruct = this.rdf.getDigitalDocument().getLogicalDocStruct();
        DocStructInterface tempChild = null;
        String fieldDocStruct = field.getDocstruct();
        if (fieldDocStruct.equals(FIRST_CHILD)) {
            try {
                tempStruct = this.rdf.getDigitalDocument().getLogicalDocStruct().getAllChildren().get(0);
            } catch (RuntimeException e) {
                Helper.setErrorMessage(
                    e.getMessage() + " The first child below the top structure could not be determined!", logger, e);
            }
        }
        // if topstruct and first child should get the metadata
        if (!fieldDocStruct.equals(FIRST_CHILD) && fieldDocStruct.contains(FIRST_CHILD)) {
            try {
                tempChild = this.rdf.getDigitalDocument().getLogicalDocStruct().getAllChildren().get(0);
            } catch (RuntimeException e) {
                Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
            }
        }
        if (fieldDocStruct.equals(BOUND_BOOK)) {
            tempStruct = this.rdf.getDigitalDocument().getPhysicalDocStruct();
        }
        // which Metadata
        try {
            // except for the authors, take all additional into the metadata
            if (!field.getMetadata().equals(LIST_OF_CREATORS)) {
                PrefsInterface prefs = serviceManager.getRulesetService()
                        .getPreferences(this.prozessKopie.getRuleset());
                MetadataTypeInterface mdt = UghHelper.getMetadataType(prefs, field.getMetadata());
                MetadataInterface metadata = UghHelper.getMetadata(tempStruct, mdt);
                if (Objects.nonNull(metadata)) {
                    metadata.setStringValue(field.getValue());
                }
                // if the topstruct and the first child should be given the value
                if (Objects.nonNull(tempChild)) {
                    metadata = UghHelper.getMetadata(tempChild, mdt);
                    if (Objects.nonNull(metadata)) {
                        metadata.setStringValue(field.getValue());
                    }
                }
            }
        } catch (UghHelperException | RuntimeException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
    }

    /**
     * There must be at least one non-anchor level doc struct,
     * if missing, insert logical doc structures until you reach it.
     */
    private void insertLogicalDocStruct() {
        DocStructInterface populizer = null;
        try {
            populizer = rdf.getDigitalDocument().getLogicalDocStruct();
            if (populizer.getAnchorClass() != null && populizer.getAllChildren() == null) {
                PrefsInterface ruleset = serviceManager.getRulesetService().getPreferences(prozessKopie.getRuleset());
                DocStructTypeInterface docStructType = populizer.getDocStructType();
                while (docStructType.getAnchorClass() != null) {
                    populizer = populizer.createChild(docStructType.getAllAllowedDocStructTypes().get(0),
                        rdf.getDigitalDocument(), ruleset);
                }
            }
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            String name = populizer != null && populizer.getDocStructType() != null
                    ? populizer.getDocStructType().getName()
                    : null;
            Helper.setErrorMessage("DocStrctType: " + name + " is configured as anchor but has no allowedchildtype.",
                logger, e);
        } catch (UGHException catchAll) {
            Helper.setErrorMessage(catchAll.getLocalizedMessage(), logger, catchAll);
        }
    }

    private void insertCollections() throws PreferencesException {
        DocStructInterface colStruct = this.rdf.getDigitalDocument().getLogicalDocStruct();
        if (Objects.nonNull(colStruct) && Objects.nonNull(colStruct.getAllChildren())
                && !colStruct.getAllChildren().isEmpty()) {
            try {
                addCollections(colStruct);
                // falls ein erstes Kind vorhanden ist, sind die Collectionen dafür
                colStruct = colStruct.getAllChildren().get(0);
                addCollections(colStruct);
            } catch (RuntimeException e) {
                Helper.setErrorMessage("The first child below the top structure could not be determined!", logger, e);
            }
        }
    }

    /**
     * Insert image path and delete any existing ones first.
     */
    private void insertImagePath() throws IOException, PreferencesException, WriteException {
        DigitalDocumentInterface digitalDocument = this.rdf.getDigitalDocument();
        try {
            MetadataTypeInterface mdt = UghHelper.getMetadataType(this.prozessKopie, "pathimagefiles");
            List<? extends MetadataInterface> allImagePaths = digitalDocument.getPhysicalDocStruct()
                    .getAllMetadataByType(mdt);
            if (Objects.nonNull(allImagePaths)) {
                for (MetadataInterface metadata : allImagePaths) {
                    digitalDocument.getPhysicalDocStruct().getAllMetadata().remove(metadata);
                }
            }
            MetadataInterface newMetadata = UghImplementation.INSTANCE.createMetadata(mdt);
            String path = serviceManager.getFileService().getImagesDirectory(this.prozessKopie)
                    + this.prozessKopie.getTitle().trim() + DIRECTORY_SUFFIX;
            if (SystemUtils.IS_OS_WINDOWS) {
                newMetadata.setStringValue("file:/" + path);
            } else {
                newMetadata.setStringValue("file://" + path);
            }
            digitalDocument.getPhysicalDocStruct().addMetadata(newMetadata);

            // write Rdf file
            serviceManager.getFileService().writeMetadataFile(this.rdf, this.prozessKopie);
        } catch (DocStructHasNoTypeException e) {
            Helper.setErrorMessage("DocStructHasNoTypeException", logger, e);
        } catch (UghHelperException e) {
            Helper.setErrorMessage("UghHelperException", logger, e);
        } catch (MetadataTypeNotAllowedException e) {
            Helper.setErrorMessage("MetadataTypeNotAllowedException", logger, e);
        }
    }

    protected void updateTasks(Process process) {
        for (Task task : process.getTasks()) {
            // always save date and user for each step
            task.setProcessingTime(process.getCreationDate());
            task.setEditTypeEnum(TaskEditType.AUTOMATIC);
            User user = serviceManager.getUserService().getAuthenticatedUser();
            serviceManager.getTaskService().replaceProcessingUser(task, user);

            // only if its done, set edit start and end date
            if (task.getProcessingStatusEnum() == TaskStatus.DONE) {
                task.setProcessingBegin(process.getCreationDate());
                // this concerns steps, which are set as done right on creation
                // bearbeitungsbeginn is set to creation timestamp of process
                // because the creation of it is basically begin of work
                Date date = new Date();
                task.setProcessingTime(date);
                task.setProcessingEnd(date);
            }
        }
    }

    /**
     * Metadata inheritance and enrichment.
     */
    private void updateMetadata() throws PreferencesException {
        if (ConfigCore.getBooleanParameter(Parameters.USE_METADATA_ENRICHMENT, false)) {
            DocStructInterface enricher = rdf.getDigitalDocument().getLogicalDocStruct();
            Map<String, Map<String, MetadataInterface>> higherLevelMetadata = new HashMap<>();
            while (enricher.getAllChildren() != null) {
                // save higher level metadata for lower enrichment
                List<MetadataInterface> allMetadata = enricher.getAllMetadata();
                if (allMetadata == null) {
                    allMetadata = Collections.emptyList();
                }
                iterateOverAllMetadata(higherLevelMetadata, allMetadata);

                // enrich children with inherited metadata
                for (DocStructInterface nextChild : enricher.getAllChildren()) {
                    enricher = nextChild;
                    iterateOverHigherLevelMetadata(enricher, higherLevelMetadata);
                }
            }
        }
    }

    private void iterateOverAllMetadata(Map<String, Map<String, MetadataInterface>> higherLevelMetadata,
            List<MetadataInterface> allMetadata) {
        for (MetadataInterface available : allMetadata) {
            String availableKey = available.getMetadataType().getName();
            String availableValue = available.getValue();
            Map<String, MetadataInterface> availableMetadata = higherLevelMetadata.containsKey(availableKey)
                    ? higherLevelMetadata.get(availableKey)
                    : new HashMap<>();
            if (!availableMetadata.containsKey(availableValue)) {
                availableMetadata.put(availableValue, available);
            }
            higherLevelMetadata.put(availableKey, availableMetadata);
        }
    }

    private void iterateOverHigherLevelMetadata(DocStructInterface enricher,
            Map<String, Map<String, MetadataInterface>> higherLevelMetadata) {
        for (Entry<String, Map<String, MetadataInterface>> availableHigherMetadata : higherLevelMetadata.entrySet()) {
            String enrichable = availableHigherMetadata.getKey();
            boolean addable = false;
            List<MetadataTypeInterface> addableTypesNotNull = enricher.getAddableMetadataTypes();
            if (Objects.isNull(addableTypesNotNull)) {
                addableTypesNotNull = Collections.emptyList();
            }
            for (MetadataTypeInterface addableMetadata : addableTypesNotNull) {
                if (addableMetadata.getName().equals(enrichable)) {
                    addable = true;
                    break;
                }
            }
            if (!addable) {
                continue;
            }
            there: for (Entry<String, MetadataInterface> higherElement : availableHigherMetadata.getValue()
                    .entrySet()) {
                List<MetadataInterface> amNotNull = enricher.getAllMetadata();
                if (Objects.isNull(amNotNull)) {
                    amNotNull = Collections.emptyList();
                }
                for (MetadataInterface existentMetadata : amNotNull) {
                    if (existentMetadata.getMetadataType().getName().equals(enrichable)
                            && existentMetadata.getValue().equals(higherElement.getKey())) {
                        continue there;
                    }
                }
                try {
                    enricher.addMetadata(higherElement.getValue());
                } catch (UGHException e) {
                    Helper.setErrorMessage("errorAdding", new Object[] {Helper.getTranslation("metadata") }, logger, e);
                }
            }
        }
    }

    private void startTaskScriptThreads() {
        /* damit die Sortierung stimmt nochmal einlesen */
        serviceManager.getProcessService().refresh(this.prozessKopie);

        List<Task> tasks = this.prozessKopie.getTasks();
        for (Task task : tasks) {
            if (task.getProcessingStatus() == 1 && task.isTypeAutomatic()) {
                TaskScriptThread thread = new TaskScriptThread(task);
                thread.start();
            }
        }
    }

    private void addCollections(DocStructInterface colStruct) {
        for (String s : this.digitalCollections) {
            try {
                MetadataInterface md = UghImplementation.INSTANCE.createMetadata(UghHelper.getMetadataType(
                    serviceManager.getRulesetService().getPreferences(this.prozessKopie.getRuleset()),
                    "singleDigCollection"));
                md.setStringValue(s);
                md.setDocStruct(colStruct);
                colStruct.addMetadata(md);
            } catch (UghHelperException | DocStructHasNoTypeException | MetadataTypeNotAllowedException e) {
                Helper.setErrorMessage(e.getMessage(), logger, e);
            }
        }
    }

    /**
     * alle Kollektionen eines übergebenen DocStructs entfernen.
     */
    protected void removeCollections(DocStructInterface colStruct, Process process) {
        try {
            MetadataTypeInterface mdt = UghHelper.getMetadataType(
                serviceManager.getRulesetService().getPreferences(process.getRuleset()), "singleDigCollection");
            ArrayList<MetadataInterface> myCollections = new ArrayList<>(colStruct.getAllMetadataByType(mdt));
            for (MetadataInterface md : myCollections) {
                colStruct.removeMetadata(md);
            }
        } catch (UghHelperException | DocStructHasNoTypeException e) {
            Helper.setErrorMessage(e.getMessage(), logger, e);
        }
    }

    /**
     * Create new file format.
     */
    public void createNewFileformat() {
        PrefsInterface myPrefs = serviceManager.getRulesetService().getPreferences(this.prozessKopie.getRuleset());
        try {
            DigitalDocumentInterface dd = UghImplementation.INSTANCE.createDigitalDocument();
            FileformatInterface ff = UghImplementation.INSTANCE.createXStream(myPrefs);
            ff.setDigitalDocument(dd);
            // add BoundBook
            DocStructTypeInterface dst = myPrefs.getDocStrctTypeByName("BoundBook");
            DocStructInterface dsBoundBook = dd.createDocStruct(dst);
            dd.setPhysicalDocStruct(dsBoundBook);

            ConfigOpacDoctype configOpacDoctype = ConfigOpac.getDoctypeByName(this.docType);

            if (configOpacDoctype != null) {
                // Monographie
                if (!configOpacDoctype.isPeriodical() && !configOpacDoctype.isMultiVolume()) {
                    DocStructTypeInterface dsty = myPrefs.getDocStrctTypeByName(configOpacDoctype.getRulesetType());
                    DocStructInterface ds = dd.createDocStruct(dsty);
                    dd.setLogicalDocStruct(ds);
                    this.rdf = ff;
                } else if (configOpacDoctype.isPeriodical()) {
                    // Zeitschrift
                    DocStructTypeInterface dsty = myPrefs.getDocStrctTypeByName("Periodical");
                    DocStructInterface ds = dd.createDocStruct(dsty);
                    dd.setLogicalDocStruct(ds);

                    DocStructTypeInterface dstyvolume = myPrefs.getDocStrctTypeByName("PeriodicalVolume");
                    DocStructInterface dsvolume = dd.createDocStruct(dstyvolume);
                    ds.addChild(dsvolume);
                    this.rdf = ff;
                } else if (configOpacDoctype.isMultiVolume()) {
                    // MultivolumeBand
                    DocStructTypeInterface dsty = myPrefs.getDocStrctTypeByName("MultiVolumeWork");
                    DocStructInterface ds = dd.createDocStruct(dsty);
                    dd.setLogicalDocStruct(ds);

                    DocStructTypeInterface dstyvolume = myPrefs.getDocStrctTypeByName("Volume");
                    DocStructInterface dsvolume = dd.createDocStruct(dstyvolume);
                    ds.addChild(dsvolume);
                    this.rdf = ff;
                }
            } else {
                // TODO: what should happen if configOpacDoctype is null?
            }

            if (this.docType.equals("volumerun")) {
                DocStructTypeInterface dsty = myPrefs.getDocStrctTypeByName("VolumeRun");
                DocStructInterface ds = dd.createDocStruct(dsty);
                dd.setLogicalDocStruct(ds);

                DocStructTypeInterface dstyvolume = myPrefs.getDocStrctTypeByName("Record");
                DocStructInterface dsvolume = dd.createDocStruct(dstyvolume);
                ds.addChild(dsvolume);
                this.rdf = ff;
            }
        } catch (TypeNotAllowedAsChildException | PreferencesException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        } catch (FileNotFoundException e) {
            Helper.setErrorMessage(ERROR_READ, new Object[] {Helper.getTranslation(OPAC_CONFIG) }, logger, e);
        }
    }

    private void addProperties() {
        addAdditionalFields(this.additionalFields, this.prozessKopie);

        for (String col : digitalCollections) {
            BeanHelper.addPropertyForProcess(this.prozessKopie, "digitalCollection", col);
        }

        BeanHelper.addPropertyForWorkpiece(this.prozessKopie, "DocType", this.docType);
        BeanHelper.addPropertyForWorkpiece(this.prozessKopie, "TifHeaderImagedescription",
            this.tifHeaderImageDescription);
        BeanHelper.addPropertyForWorkpiece(this.prozessKopie, "TifHeaderDocumentname", this.tifHeaderDocumentName);
        BeanHelper.addPropertyForProcess(this.prozessKopie, "Template", this.template.getTitle());
        BeanHelper.addPropertyForProcess(this.prozessKopie, "TemplateID", String.valueOf(this.template.getId()));
    }

    protected void addAdditionalFields(List<AdditionalField> additionalFields, Process process) {
        for (AdditionalField field : additionalFields) {
            if (field.getShowDependingOnDoctype()) {
                if (field.getFrom().equals("werk")) {
                    BeanHelper.addPropertyForWorkpiece(process, field.getTitle(), field.getValue());
                }
                if (field.getFrom().equals("vorlage")) {
                    BeanHelper.addPropertyForTemplate(process, field.getTitle(), field.getValue());
                }
                if (field.getFrom().equals("prozess")) {
                    BeanHelper.addPropertyForProcess(process, field.getTitle(), field.getValue());
                }
            }
        }
    }

    public String getDocType() {
        return this.docType;
    }

    /**
     * Set document type.
     *
     * @param docType
     *            String
     */
    public void setDocType(String docType) {
        if (!this.docType.equals(docType)) {
            this.docType = docType;
            if (rdf != null) {

                FileformatInterface tmp = rdf;

                createNewFileformat();
                try {
                    if (rdf.getDigitalDocument().getLogicalDocStruct()
                            .equals(tmp.getDigitalDocument().getLogicalDocStruct())) {
                        rdf = tmp;
                    } else {
                        DocStructInterface oldLogicalDocstruct = tmp.getDigitalDocument().getLogicalDocStruct();
                        DocStructInterface newLogicalDocstruct = rdf.getDigitalDocument().getLogicalDocStruct();
                        // both have no children
                        if (oldLogicalDocstruct.getAllChildren() == null
                                && newLogicalDocstruct.getAllChildren() == null) {
                            copyMetadata(oldLogicalDocstruct, newLogicalDocstruct);
                        } else if (oldLogicalDocstruct.getAllChildren() != null
                                && newLogicalDocstruct.getAllChildren() == null) {
                            // old has a child, new has no child
                            copyMetadata(oldLogicalDocstruct, newLogicalDocstruct);
                            copyMetadata(oldLogicalDocstruct.getAllChildren().get(0), newLogicalDocstruct);
                        } else if (oldLogicalDocstruct.getAllChildren() == null
                                && newLogicalDocstruct.getAllChildren() != null) {
                            // new has a child, but old not
                            copyMetadata(oldLogicalDocstruct, newLogicalDocstruct);
                            copyMetadata(oldLogicalDocstruct.copy(true, false),
                                newLogicalDocstruct.getAllChildren().get(0));
                        } else if (oldLogicalDocstruct.getAllChildren() != null
                                && newLogicalDocstruct.getAllChildren() != null) {
                            // both have children
                            copyMetadata(oldLogicalDocstruct, newLogicalDocstruct);
                            copyMetadata(oldLogicalDocstruct.getAllChildren().get(0),
                                newLogicalDocstruct.getAllChildren().get(0));
                        }
                    }
                } catch (PreferencesException e) {
                    Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
                }
                try {
                    fillFieldsFromMetadataFile();
                } catch (PreferencesException e) {
                    Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
                }
            }
        }
    }

    private void copyMetadata(DocStructInterface oldDocStruct, DocStructInterface newDocStruct) {

        if (oldDocStruct.getAllMetadata() != null) {
            for (MetadataInterface md : oldDocStruct.getAllMetadata()) {
                try {
                    newDocStruct.addMetadata(md);
                } catch (MetadataTypeNotAllowedException | DocStructHasNoTypeException e) {
                    Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
                }
            }
        }
        if (oldDocStruct.getAllPersons() != null) {
            for (PersonInterface p : oldDocStruct.getAllPersons()) {
                try {
                    newDocStruct.addPerson(p);
                } catch (MetadataTypeNotAllowedException | DocStructHasNoTypeException e) {
                    Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
                }
            }
        }
    }

    /**
     * Get template.
     *
     * @return value of template
     */
    public Template getTemplate() {
        return template;
    }

    /**
     * The function getProzessVorlageTitel() returns some kind of identifier for
     * this ProzesskopieForm. The title of the process template that a process
     * will be created from can be considered with some reason to be some good
     * identifier for the ProzesskopieForm, too.
     *
     * @return a human-readable identifier for this object
     */
    public String getProzessVorlageTitel() {
        return this.template != null ? this.template.getTitle() : null;
    }

    /**
     * Set template.
     *
     * @param template
     *            as Template object
     */
    public void setTemplate(Template template) {
        this.template = template;
    }

    public Integer getAuswahl() {
        return this.auswahl;
    }

    public void setAuswahl(Integer auswahl) {
        this.auswahl = auswahl;
    }

    public List<AdditionalField> getAdditionalFields() {
        return this.additionalFields;
    }

    /**
     * The method getVisibleAdditionalFields returns a list of visible
     * additional fields.
     *
     * @return list of AdditionalField
     */
    public List<AdditionalField> getVisibleAdditionalFields() {
        return this.getAdditionalFields().stream().filter(AdditionalField::getShowDependingOnDoctype)
                .collect(Collectors.toList());
    }

    /**
     * The method setAdditionalField() sets the value of an AdditionalField held
     * by a ProzesskopieForm object.
     *
     * @param key
     *            the title of the AdditionalField whose value shall be modified
     * @param value
     *            the new value for the AdditionalField
     * @param strict
     *            throw a RuntimeException if the field is unknown
     * @throws ProcessCreationException
     *             in case that no field with a matching title was found in the
     *             ProzesskopieForm object
     */
    public void setAdditionalField(String key, String value, boolean strict) {
        boolean unknownField = true;
        for (AdditionalField field : additionalFields) {
            if (key.equals(field.getTitle())) {
                field.setValue(value);
                unknownField = false;
            }
        }
        if (unknownField && strict) {
            throw new ProcessCreationException(
                    "Couldn’t set “" + key + "” to “" + value + "”: No such field in record.");
        }
    }

    public void setAdditionalFields(List<AdditionalField> additionalFields) {
        this.additionalFields = additionalFields;
    }

    /**
     * This is needed for GUI, render multiple select only if this is false if
     * this is true use the only choice.
     *
     * @return true or false
     */
    public boolean isSingleChoiceCollection() {
        return (getPossibleDigitalCollections() != null && getPossibleDigitalCollections().size() == 1);
    }

    /**
     * Get possible digital collections if single choice.
     *
     * @return possible digital collections if single choice
     */
    public String getDigitalCollectionIfSingleChoice() {
        List<String> pdc = getPossibleDigitalCollections();
        if (pdc.size() == 1) {
            return pdc.get(0);
        } else {
            return null;
        }
    }

    public List<String> getPossibleDigitalCollections() {
        return this.possibleDigitalCollection;
    }

    @SuppressWarnings("unchecked")
    private void initializePossibleDigitalCollections() {
        try {
            DigitalCollections.possibleDigitalCollectionsForProcess(this.prozessKopie);
        } catch (JDOMException | IOException e) {
            Helper.setErrorMessage("Error while parsing digital collections", logger, e);
        }

        this.possibleDigitalCollection = DigitalCollections.getPossibleDigitalCollection();
        this.digitalCollections = DigitalCollections.getDigitalCollections();

        // if only one collection is possible take it directly

        if (isSingleChoiceCollection()) {
            this.digitalCollections.add(getDigitalCollectionIfSingleChoice());
        }
    }

    /**
     * Get all OPAC catalogues.
     *
     * @return list of catalogues
     */
    public List<String> getAllOpacCatalogues() {
        try {
            return ConfigOpac.getAllCatalogueTitles();
        } catch (RuntimeException e) {
            Helper.setErrorMessage(ERROR_READ, new Object[] {Helper.getTranslation(OPAC_CONFIG) }, logger, e);
            return new ArrayList<>();
        }
    }

    /**
     * Get all document types.
     *
     * @return list of ConfigOpacDoctype objects
     */
    public List<ConfigOpacDoctype> getAllDoctypes() {
        try {
            return ConfigOpac.getAllDoctypes();
        } catch (RuntimeException e) {
            Helper.setErrorMessage(ERROR_READ, new Object[] {Helper.getTranslation(OPAC_CONFIG) }, logger, e);
            return new ArrayList<>();
        }
    }

    /**
     * Changed, so that on first request list gets set if there is only one choice.
     *
     * @return list of digital collections
     */
    public List<String> getDigitalCollections() {
        return this.digitalCollections;
    }

    public void setDigitalCollections(List<String> digitalCollections) {
        this.digitalCollections = digitalCollections;
    }

    public Map<String, Boolean> getStandardFields() {
        return this.standardFields;
    }

    public boolean isUseOpac() {
        return this.useOpac;
    }

    public boolean isUseTemplates() {
        return this.useTemplates;
    }

    public String getTifHeaderDocumentName() {
        return this.tifHeaderDocumentName;
    }

    public void setTifHeaderDocumentName(String tifHeaderDocumentName) {
        this.tifHeaderDocumentName = tifHeaderDocumentName;
    }

    public String getTifHeaderImageDescription() {
        return this.tifHeaderImageDescription;
    }

    public void setTifHeaderImageDescription(String tifHeaderImageDescription) {
        this.tifHeaderImageDescription = tifHeaderImageDescription;
    }

    public Process getProzessKopie() {
        return this.prozessKopie;
    }

    public void setProzessKopie(Process prozessKopie) {
        this.prozessKopie = prozessKopie;
    }

    public String getOpacSuchfeld() {
        return this.opacSuchfeld;
    }

    public void setOpacSuchfeld(String opacSuchfeld) {
        this.opacSuchfeld = opacSuchfeld;
    }

    public String getOpacKatalog() {
        return this.opacKatalog;
    }

    public void setOpacKatalog(String opacKatalog) {
        this.opacKatalog = opacKatalog;
    }

    public String getOpacSuchbegriff() {
        return this.opacSuchbegriff;
    }

    public void setOpacSuchbegriff(String opacSuchbegriff) {
        this.opacSuchbegriff = opacSuchbegriff;
    }

    /*
     * Helper
     */

    /**
     * Prozesstitel und andere Details generieren.
     */
    public void calculateProcessTitle() {
        try {
            generateTitle(null);
            Ajax.update("editForm");
        } catch (IOException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
        }
    }

    /**
     * Generate title.
     *
     * @param genericFields
     *            Map of Strings
     * @return String
     */
    public String generateTitle(Map<String, String> genericFields) throws IOException {
        String currentAuthors = "";
        String currentTitle = "";
        int counter = 0;
        for (AdditionalField field : this.additionalFields) {
            if (field.getAutogenerated() && field.getValue().isEmpty()) {
                field.setValue(String.valueOf(System.currentTimeMillis() + counter));
                counter++;
            }
            if (field.getMetadata() != null && field.getMetadata().equals("TitleDocMain")
                    && currentTitle.length() == 0) {
                currentTitle = field.getValue();
            } else if (field.getMetadata() != null && field.getMetadata().equals(LIST_OF_CREATORS)
                    && currentAuthors.length() == 0) {
                currentAuthors = field.getValue();
            }

        }
        StringBuilder newTitle = new StringBuilder();
        String titleDefinition = getTitleDefinition(this.template.getProject().getTitle(), this.docType);

        StringTokenizer tokenizer = new StringTokenizer(titleDefinition, "+");
        /* jetzt den Bandtitel parsen */
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            /*
             * wenn der String mit ' anfängt und mit ' endet, dann den Inhalt so
             * übernehmen
             */
            if (token.startsWith("'") && token.endsWith("'")) {
                newTitle.append(token, 1, token.length() - 1);
            } else if (token.startsWith("#")) {
                // resolve strings beginning with # from generic fields
                if (genericFields != null) {
                    String genericValue = genericFields.get(token);
                    if (genericValue != null) {
                        newTitle.append(genericValue);
                    }
                }
            } else {
                newTitle.append(evaluateAdditionalFieldsForTitle(currentTitle, currentAuthors, token));
            }
        }

        if (newTitle.toString().endsWith("_")) {
            newTitle.setLength(newTitle.length() - 1);
        }
        // remove non-ascii characters for the sake of TIFF header limits
        String filteredTitle = newTitle.toString().replaceAll("[^\\p{ASCII}]", "");
        prozessKopie.setTitle(filteredTitle);
        calculateTiffHeader();
        return filteredTitle;
    }

    protected String getTitleDefinition(String projectTitle, String docType) throws IOException {
        ConfigProjects cp = new ConfigProjects(projectTitle);
        int count = cp.getParamList(ITEM_LIST_PROCESS_TITLE).size();
        String titleDefinition = "";

        for (int i = 0; i < count; i++) {
            String title = cp.getParamString(ITEM_LIST_PROCESS_TITLE + "(" + i + ")");
            String isDocType = cp.getParamString(ITEM_LIST_PROCESS_TITLE + "(" + i + ")[@isdoctype]");
            String isNotDocType = cp.getParamString(ITEM_LIST_PROCESS_TITLE + "(" + i + ")[@isnotdoctype]");

            title = processNullValues(title);
            isDocType = processNullValues(isDocType);
            isNotDocType = processNullValues(isNotDocType);

            titleDefinition = findTitleDefinition(title, docType, isDocType, isNotDocType);

            // break loop after title definition was found
            if (isTitleDefinitionFound(titleDefinition)) {
                break;
            }
        }
        return titleDefinition;
    }

    /**
     * Conditions:
     * isDocType.equals("") && isNotDocType.equals("")
     *     - nothing was specified
     * isNotDocType.equals("") && StringUtils.containsIgnoreCase(isDocType, docType)
     *     - only duty was specified
     * isDocType.equals("") && !StringUtils.containsIgnoreCase(isNotDocType, docType)
     *    - only may not was specified
     * !isDocType.equals("") && !isNotDocType.equals("") && StringUtils.containsIgnoreCase(isDocType, docType)
     *                 && !StringUtils.containsIgnoreCase(isNotDocType, docType)
     *    - both were specified
     */
    private String findTitleDefinition(String title, String docType, String isDocType, String isNotDocType) {
        if ((isDocType.equals("")
                && (isNotDocType.equals("") || !StringUtils.containsIgnoreCase(isNotDocType, docType)))
                || (!isDocType.equals("") && !isNotDocType.equals("")
                && StringUtils.containsIgnoreCase(isDocType, docType)
                && !StringUtils.containsIgnoreCase(isNotDocType, docType))
                || (isNotDocType.equals("") && StringUtils.containsIgnoreCase(isDocType, docType))) {
            return title;
        }
        return "";
    }

    private boolean isTitleDefinitionFound(String titleDefinition) {
        return !titleDefinition.equals("");
    }

    private String evaluateAdditionalFieldsForTitle(String currentTitle, String currentAuthors, String token) {
        StringBuilder newTitle = new StringBuilder();

        for (AdditionalField additionalField : this.additionalFields) {
            /*
             * if it is the ATS or TSL field, then use the calculated
             * atstsl if it does not already exist
             */
            if ((additionalField.getTitle().equals("ATS") || additionalField.getTitle().equals("TSL"))
                    && additionalField.getShowDependingOnDoctype()
                    && (additionalField.getValue() == null || additionalField.getValue().equals(""))) {
                if (atstsl == null || atstsl.length() == 0) {
                    atstsl = createAtstsl(currentTitle, currentAuthors);
                }
                additionalField.setValue(this.atstsl);
            }

            // add the content to the title
            if (additionalField.getTitle().equals(token) && additionalField.getShowDependingOnDoctype()
                    && additionalField.getValue() != null) {
                newTitle.append(calculateProcessTitleCheck(additionalField.getTitle(), additionalField.getValue()));
            }
        }
        return newTitle.toString();
    }

    private String processNullValues(String value) {
        if (value == null) {
            value = "";
        }
        return value;
    }

    private String calculateProcessTitleCheck(String inFeldName, String inFeldWert) {
        String rueckgabe = inFeldWert;

        // Bandnummer
        if (inFeldName.equals("Bandnummer") || inFeldName.equals("Volume number")) {
            try {
                int bandint = Integer.parseInt(inFeldWert);
                java.text.DecimalFormat df = new java.text.DecimalFormat("#0000");
                rueckgabe = df.format(bandint);
            } catch (NumberFormatException e) {
                if (inFeldName.equals("Bandnummer")) {
                    Helper.setErrorMessage(Helper.getTranslation(INCOMPLETE_DATA) + "Bandnummer ist keine gültige Zahl",
                        logger, e);
                } else {
                    Helper.setErrorMessage(
                        Helper.getTranslation(INCOMPLETE_DATA) + "Volume number is not a valid number", logger, e);
                }
            }
            if (rueckgabe != null && rueckgabe.length() < 4) {
                rueckgabe = "0000".substring(rueckgabe.length()) + rueckgabe;
            }
        }

        return rueckgabe;
    }

    /**
     * Calculate tiff header.
     */
    public void calculateTiffHeader() {
        ConfigProjects cp;
        try {
            cp = new ConfigProjects(this.template.getProject().getTitle());
        } catch (IOException e) {
            Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
            return;
        }
        String tifDefinition = cp.getParamString("tifheader." + this.docType, "intranda");

        // possible replacements
        tifDefinition = tifDefinition.replaceAll("\\[\\[", "<");
        tifDefinition = tifDefinition.replaceAll("\\]\\]", ">");

        // Documentname ist im allgemeinen = Prozesstitel
        this.tifHeaderDocumentName = this.prozessKopie.getTitle();
        this.tifHeaderImageDescription = "";
        // image description
        StringTokenizer tokenizer = new StringTokenizer(tifDefinition, "+");
        // jetzt den Tiffheader parsen
        String title = "";
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            /*
             * wenn der String mit ' anfängt und mit ' endet, dann den Inhalt so
             * übernehmen
             */
            if (token.startsWith("'") && token.endsWith("'") && token.length() > 2) {
                this.tifHeaderImageDescription += token.substring(1, token.length() - 1);
            } else if (token.equals("$Doctype")) {
                /* wenn der Doctype angegeben werden soll */
                try {
                    this.tifHeaderImageDescription += ConfigOpac.getDoctypeByName(this.docType).getTifHeaderType();
                } catch (FileNotFoundException | RuntimeException e) {
                    Helper.setErrorMessage(ERROR_READ, new Object[] {Helper.getTranslation(OPAC_CONFIG) }, logger, e);
                }
            } else {
                /* andernfalls den string als Feldnamen auswerten */
                for (AdditionalField additionalField : this.additionalFields) {
                    if (additionalField.getTitle().equals("Titel") || additionalField.getTitle().equals("Title")
                            && additionalField.getValue() != null && !additionalField.getValue().equals("")) {
                        title = additionalField.getValue();
                    }
                    /*
                     * wenn es das ATS oder TSL-Feld ist, dann den berechneten
                     * atstsl einsetzen, sofern noch nicht vorhanden
                     */
                    if ((additionalField.getTitle().equals("ATS") || additionalField.getTitle().equals("TSL"))
                            && additionalField.getShowDependingOnDoctype()
                            && (additionalField.getValue() == null || additionalField.getValue().equals(""))) {
                        additionalField.setValue(this.atstsl);
                    }

                    /* den Inhalt zum Titel hinzufügen */
                    if (additionalField.getTitle().equals(token) && additionalField.getShowDependingOnDoctype()
                            && additionalField.getValue() != null) {
                        this.tifHeaderImageDescription += calculateProcessTitleCheck(additionalField.getTitle(),
                            additionalField.getValue());
                    }

                }
            }
        }
        reduceLengthOfTifHeaderImageDescription(title);
    }

    /**
     * Reduce to 255 characters.
     */
    private void reduceLengthOfTifHeaderImageDescription(String title) {
        int length = this.tifHeaderImageDescription.length();
        if (length > 255) {
            try {
                int toCut = length - 255;
                String newTitle = title.substring(0, title.length() - toCut);
                this.tifHeaderImageDescription = this.tifHeaderImageDescription.replace(title, newTitle);
            } catch (IndexOutOfBoundsException e) {
                Helper.setErrorMessage(e.getLocalizedMessage(), logger, e);
            }
        }
    }

    /**
     * Downloads a docket for the process.
     *
     * @return the navigation-strign
     */
    public String downloadDocket() {
        try {
            serviceManager.getProcessService().downloadDocket(this.prozessKopie);
        } catch (IOException e) {
            Helper.setErrorMessage("errorCreating", new Object[] {Helper.getTranslation("docket") }, logger, e);
        }
        return "";
    }

    /**
     * Set images guessed.
     *
     * @param imagesGuessed
     *            the imagesGuessed to set
     */
    public void setImagesGuessed(Integer imagesGuessed) {
        if (imagesGuessed == null) {
            imagesGuessed = 0;
        }
        this.guessedImages = imagesGuessed;
    }

    /**
     * Get images guessed.
     *
     * @return the imagesGuessed
     */
    public Integer getImagesGuessed() {
        return this.guessedImages;
    }

    public String getAddToWikiField() {
        return this.addToWikiField;
    }

    /**
     * Set add to wiki field.
     *
     * @param addToWikiField
     *            String
     */
    public void setAddToWikiField(String addToWikiField) {
        this.prozessKopie.setWikiField(this.template.getWikiField());
        this.addToWikiField = addToWikiField;
        if (addToWikiField != null && !addToWikiField.equals("")) {
            User user = serviceManager.getUserService().getAuthenticatedUser();
            String message = this.addToWikiField + " (" + serviceManager.getUserService().getFullName(user) + ")";
            this.prozessKopie
                    .setWikiField(WikiFieldHelper.getWikiMessage(prozessKopie.getWikiField(), "info", message));
        }
    }

    /**
     * Create Atstsl.
     *
     * @param title
     *            String
     * @param author
     *            String
     * @return String
     */
    public static String createAtstsl(String title, String author) {
        StringBuilder result = new StringBuilder(8);
        if (author != null && author.trim().length() > 0) {
            result.append(getPartString(author, 4));
            result.append(getPartString(title, 4));
        } else {
            StringTokenizer titleWords = new StringTokenizer(title);
            int wordNo = 1;
            while (titleWords.hasMoreTokens() && wordNo < 5) {
                String word = titleWords.nextToken();
                switch (wordNo) {
                    case 1:
                        result.append(getPartString(word, 4));
                        break;
                    case 2:
                    case 3:
                        result.append(getPartString(word, 2));
                        break;
                    case 4:
                        result.append(getPartString(word, 1));
                        break;
                    default:
                        assert false : wordNo;
                }
                wordNo++;
            }
        }
        return result.toString().replaceAll("[\\W]", ""); // delete umlauts etc.
    }

    private static String getPartString(String word, int length) {
        return word.length() > length ? word.substring(0, length) : word;
    }

    /**
     * The function getHitlist returns the hits for the currently showing page of
     * the hitlist as read-only property "hitlist".
     *
     * @return a list of hits to render in the hitlist
     */
    public List<SelectableHit> getHitlist() {
        if (hitlistPage < 0) {
            return Collections.emptyList();
        }
        int pageSize = getPageSize();
        List<SelectableHit> result = new ArrayList<>(pageSize);
        long firstHit = hitlistPage * pageSize;
        long lastHit = Math.min(firstHit + pageSize - 1, hits - 1);
        for (long index = firstHit; index <= lastHit; index++) {
            try {
                Hit hit = importCatalogue.getHit(hitlist, index, CataloguePlugin.getTimeout());
                result.add(new SelectableHit(hit));
            } catch (RuntimeException e) {
                result.add(new SelectableHit(e.getMessage()));
            }
        }
        return result;
    }

    /**
     * The function getNumberOfHits() returns the number of hits on the hit list
     * as read-only property "numberOfHits".
     *
     * @return the number of hits on the hit list
     */
    public long getNumberOfHits() {
        return hits;
    }

    /**
     * The function getPageSize() retrieves the desired number of hits on one
     * page of the hit list from the configuration.
     *
     * @return desired number of hits on one page of the hit list from the
     *         configuration
     */
    private int getPageSize() {
        return ConfigCore.getIntParameter(Parameters.HITLIST_PAGE_SIZE, DEFAULT_HITLIST_PAGE_SIZE);
    }

    /**
     * The function isFirstPage() returns whether the currently showing page of
     * the hitlist is the first page of it as read-only property "firstPage".
     *
     * @return whether the currently showing page of the hitlist is the first
     *         one
     */
    public boolean isFirstPage() {
        return hitlistPage == 0;
    }

    /**
     * The function getHitlistShowing returns whether the hitlist shall be
     * rendered or not as read-only property "hitlistShowing".
     *
     * @return whether the hitlist is to be shown or not
     */
    public boolean isHitlistShowing() {
        return hitlistPage >= 0;
    }

    /**
     * The function isLastPage() returns whether the currently showing page of
     * the hitlist is the last page of it as read-only property "lastPage".
     *
     * @return whether the currently showing page of the hitlist is the last one
     */
    public boolean isLastPage() {
        return (hitlistPage + 1) * getPageSize() > hits - 1;
    }

    /**
     * The function nextPageClick() is executed if the user clicks the action
     * link to flip one page forward in the hit list.
     */
    public void nextPageClick() {
        hitlistPage++;
    }

    /**
     * The function previousPageClick() is executed if the user clicks the
     * action link to flip one page backwards in the hit list.
     */
    public void previousPageClick() {
        hitlistPage--;
    }

    /**
     * The function isCalendarButtonShowing tells whether the calendar button
     * shall show up or not as read-only property "calendarButtonShowing".
     *
     * @return whether the calendar button shall show
     */
    public boolean isCalendarButtonShowing() {
        try {
            return ConfigOpac.getDoctypeByName(docType).isNewspaper();
        } catch (NullPointerException e) {
            // may occur if user continues to interact with the page across a
            // restart of the servlet container
            return false;
        } catch (FileNotFoundException e) {
            Helper.setErrorMessage(ERROR_READ, new Object[] {Helper.getTranslation(OPAC_CONFIG) }, logger, e);
            return false;
        }
    }

    /**
     * Returns the representation of the file holding the document metadata in
     * memory.
     *
     * @return the metadata file in memory
     */
    public FileformatInterface getFileformat() {
        return rdf;
    }

    /**
     * Get workflow conditions.
     *
     * @return value of workflowConditions
     */
    public List<String> getWorkflowConditions() {
        return workflowConditions;
    }

    /**
     * Set workflow conditions.
     *
     * @param workflowConditions
     *            as List of Strings
     */
    public void setWorkflowConditions(List<String> workflowConditions) {
        this.workflowConditions = workflowConditions;
    }

    /**
     * Get ruleset ID.
     * @return ruleset ID.
     */
    public int getRulesetID() {
        if (Objects.nonNull(this.prozessKopie) && Objects.nonNull(this.prozessKopie.getRuleset())) {
            return this.prozessKopie.getRuleset().getId();
        } else {
            return 0;
        }
    }

    /**
     * Set ruleset by ID.
     * @param id ruleset ID.
     */
    public void setRulesetID(int id) {
        if (!this.prozessKopie.getRuleset().getId().equals(id)) {
            try {
                this.prozessKopie.setRuleset(serviceManager.getRulesetService().getById(id));
            } catch (DAOException e) {
                Helper.setErrorMessage("ERROR: unable to save ruleset to process copy!");
            }
        }
    }
}
