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
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.Page;
import de.sub.goobi.helper.ldap.Ldap;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.inject.Named;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.kitodo.api.frontend.FrontendInterface;
import org.kitodo.data.database.beans.LdapGroup;
import org.kitodo.data.database.beans.Project;
import org.kitodo.data.database.beans.User;
import org.kitodo.data.database.beans.UserGroup;
import org.kitodo.data.database.exceptions.DAOException;
import org.kitodo.data.exceptions.DataException;
import org.kitodo.dto.ProjectDTO;
import org.kitodo.dto.UserDTO;
import org.kitodo.dto.UserGroupDTO;
import org.kitodo.model.LazyDTOModel;
import org.kitodo.security.SecurityPasswordEncoder;
import org.kitodo.serviceloader.KitodoServiceLoader;
import org.kitodo.services.ServiceManager;

@Named("BenutzerverwaltungForm")
@SessionScoped
public class BenutzerverwaltungForm extends BasisForm {
    private static final long serialVersionUID = -3635859455444639614L;
    private User userObject = new User();
    private boolean hideInactiveUsers = true;
    private transient ServiceManager serviceManager = new ServiceManager();
    private static final Logger logger = LogManager.getLogger(BenutzerverwaltungForm.class);
    private int userId;
    private SecurityPasswordEncoder passwordEncoder = new SecurityPasswordEncoder();
    private String password;

    /**
     * Empty default constructor that also sets the LazyDTOModel instance of this
     * bean.
     */
    public BenutzerverwaltungForm() {
        super();
        super.setLazyDTOModel(new LazyDTOModel(serviceManager.getUserService()));
    }

    /**
     * New user.
     *
     * @return page
     */
    public String newUser() {
        this.userObject = new User();
        this.userObject.setName("");
        this.userObject.setSurname("");
        this.userObject.setLogin("");
        this.userObject.setLdapLogin("");
        this.userObject.setPassword("");
        this.userId = 0;
        this.password = "";
        return redirectToEdit("?faces-redirect=true");
    }

    /**
     * display all users without filtering.
     *
     * @return page or empty String
     */
    public String filterKein() {
        this.filter = null;
        List<UserDTO> users = new ArrayList<>();
        try {
            users = getUsers();
        } catch (DataException e) {
            logger.error(e);
        }
        this.page = new Page<>(0, users);
        return "/pages/BenutzerAlle";
    }

    /**
     * This method initializes the user list without any filters whenever the
     * bean is constructed.
     */
    @PostConstruct
    public void initializeUserList() throws IOException, URISyntaxException {
        initialiseFrontendModule();
        filterKein();
    }

    private FrontendInterface initialiseFrontendModule() {
        KitodoServiceLoader<FrontendInterface> loader = new KitodoServiceLoader<>(FrontendInterface.class,
                ConfigCore.getParameter("moduleFolder"));
        return loader.loadModule();
    }

    public String filterKeinMitZurueck() {
        filterKein();
        return this.zurueck;
    }

    /**
     * Anzeige der gefilterten Nutzer.
     */
    public String filterAll() {
        List<UserDTO> users = new ArrayList<>();
        try {
            if (this.filter != null && this.filter.length() != 0) {
                users = serviceManager.getUserService().findActiveUsersByName(this.filter);
            } else {
                users = getUsers();
            }
        } catch (DataException e) {
            logger.error(e);
        }
        this.page = new Page<>(0, users);
        return "/pages/BenutzerAlle";
    }

    private List<UserDTO> getUsers() throws DataException {
        if (this.hideInactiveUsers) {
            return serviceManager.getUserService().findAllActiveUsers();
        } else {
            return serviceManager.getUserService().findAllVisibleUsers();
        }
    }

    /**
     * Save user if there is not other user with the same login.
     *
     * @return page or empty String
     */
    public String save() {
        Session session = Helper.getHibernateSession();
        session.evict(this.userObject);
        String login = this.userObject.getLogin();

        if (!isLoginValid(login)) {
            return null;
        }

        String id = null;
        if (this.userObject.getId() != null) {
            id = this.userObject.getId().toString();
        }

        try {
            if (this.serviceManager.getUserService().getAmountOfUsersWithExactlyTheSameLogin(id, login) == 0) {
                this.userObject.setPassword(passwordEncoder.encrypt(this.password));
                this.serviceManager.getUserService().save(this.userObject);
                return redirectToList("?faces-redirect=true");
            } else {
                Helper.setFehlerMeldung("", Helper.getTranslation("loginBereitsVergeben"));
                return null;
            }
        } catch (DataException e) {
            Helper.setFehlerMeldung("Error, could not save", e.getMessage());
            logger.error(e);
            return null;
        }
    }

    private boolean isLoginValid(String inLogin) {
        boolean valide = true;
        String patternStr = "[A-Za-z0-9@_\\-.]*";
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(inLogin);
        valide = matcher.matches();
        if (!valide) {
            Helper.setFehlerMeldung("", Helper.getTranslation("loginNotValid"));
        }

        /* Pfad zur Datei ermitteln */
        FacesContext context = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) context.getExternalContext().getSession(false);
        String filename = session.getServletContext().getRealPath("/WEB-INF") + File.separator + "classes"
                + File.separator + "kitodo_loginBlacklist.txt";
        /*
         * Datei zeilenweise durchlaufen und die auf ungültige Zeichen
         * vergleichen
         */
        try (FileInputStream fis = new FileInputStream(filename);
                InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                BufferedReader in = new BufferedReader(isr)) {
            String str;
            while ((str = in.readLine()) != null) {
                if (str.length() > 0 && inLogin.equalsIgnoreCase(str)) {
                    valide = false;
                    Helper.setFehlerMeldung("", "Login " + str + Helper.getTranslation("loginNotValid"));
                }
            }
        } catch (IOException e) {
            logger.error(e);
        }
        return valide;
    }

    /**
     * The function delete() deletes a user account.
     *
     * <p>
     * Please note that deleting a user in goobi.production will not delete the
     * user from a connected LDAP service.
     * </p>
     *
     * @return a string indicating the screen showing up after the command has
     *         been performed.
     */
    public String delete() {
        try {
            serviceManager.getUserService().remove(userObject);
        } catch (DataException e) {
            Helper.setFehlerMeldung("Error, could not save", e.getMessage());
            logger.error(e);
            return null;
        }
        return filterKein();
    }

    /**
     * Remove from user group.
     *
     * @return empty String
     */
    public String deleteFromGroup() {
        int gruppenID = Integer.parseInt(Helper.getRequestParameter("ID"));

        List<UserGroup> neu = new ArrayList<>();
        for (UserGroup userGroup : this.userObject.getUserGroups()) {
            if (userGroup.getId() != gruppenID) {
                neu.add(userGroup);
            }
        }
        this.userObject.setUserGroups(neu);
        return null;
    }

    /**
     * Add to user group.
     *
     * @return empty String or null
     */
    public String addToGroup() {
        Integer gruppenID = Integer.valueOf(Helper.getRequestParameter("ID"));
        try {
            UserGroup usergroup = serviceManager.getUserGroupService().getById(gruppenID);
            for (UserGroup b : this.userObject.getUserGroups()) {
                if (b.equals(usergroup)) {
                    return null;
                }
            }
            this.userObject.getUserGroups().add(usergroup);
        } catch (DAOException e) {
            Helper.setFehlerMeldung("Error on reading database", e.getMessage());
            return null;
        }
        return null;
    }

    /**
     * Remove user from project.
     *
     * @return empty String
     */
    public String deleteFromProject() {
        int projectId = Integer.parseInt(Helper.getRequestParameter("ID"));
        try {
            Project project = serviceManager.getProjectService().getById(projectId);
            this.userObject.getProjects().remove(project);
        } catch (DAOException e) {
            Helper.setFehlerMeldung("Error on reading database", e.getMessage());
            return null;
        }
        return null;
    }

    /**
     * Add user to project.
     *
     * @return empty String or null
     */
    public String addToProject() {
        Integer projectId = Integer.valueOf(Helper.getRequestParameter("ID"));
        try {
            Project project = serviceManager.getProjectService().getById(projectId);
            for (Project p : this.userObject.getProjects()) {
                if (p.equals(project)) {
                    return null;
                }
            }
            this.userObject.getProjects().add(project);
        } catch (DAOException e) {
            Helper.setFehlerMeldung("Error on reading database", e.getMessage());
            return null;
        }
        return null;
    }

    /*
     * Getter und Setter
     */

    public User getUserObject() {
        return this.userObject;
    }

    /**
     * Set class.
     *
     * @param userObject
     *            user object
     */
    public void setUserObject(User userObject) {
        try {
            this.userObject = serviceManager.getUserService().getById(userObject.getId());
        } catch (DAOException e) {
            this.userObject = userObject;
        }
    }

    /**
     * Ldap-Konfiguration - choose LDAP group.
     */
    public Integer getLdapGruppeAuswahl() {
        if (this.userObject.getLdapGroup() != null) {
            return this.userObject.getLdapGroup().getId();
        } else {
            return 0;
        }
    }

    /**
     * Ldap-Konfiguration - set LDAP group.
     */
    public void setLdapGruppeAuswahl(Integer inAuswahl) {
        if (inAuswahl != 0) {
            try {
                this.userObject.setLdapGroup(serviceManager.getLdapGroupService().getById(inAuswahl));
            } catch (DAOException e) {
                Helper.setFehlerMeldung("Error on writing to database", "");
                logger.error(e);
            }
        }
    }

    /**
     * Ldap-Konfiguration - get LDAP group choice list.
     */
    public List<SelectItem> getLdapGruppeAuswahlListe() {
        List<SelectItem> myLdapGruppen = new ArrayList<>();
        List<LdapGroup> temp = serviceManager.getLdapGroupService().getByQuery("from LdapGroup ORDER BY title");
        for (LdapGroup gru : temp) {
            myLdapGruppen.add(new SelectItem(gru.getId(), gru.getTitle(), null));
        }
        return myLdapGruppen;
    }

    /**
     * Ldap-Konfiguration für den Benutzer schreiben.
     */
    public String ldapKonfigurationSchreiben() {
        Ldap myLdap = new Ldap();
        try {
            myLdap.createNewUser(this.userObject, passwordEncoder.decrypt(this.userObject.getPassword()));
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Could not generate ldap entry: " + e.getMessage());
            }
            Helper.setFehlerMeldung(e.getMessage());
        }
        return null;
    }

    public boolean isHideInactiveUsers() {
        return this.hideInactiveUsers;
    }

    public void setHideInactiveUsers(boolean hideInactiveUsers) {
        this.hideInactiveUsers = hideInactiveUsers;
    }

    public boolean getLdapUsage() {
        return ConfigCore.getBooleanParameter("ldap_use");
    }

    /**
     * Method being used as viewAction for user edit form. If 'userId' is '0',
     * the form for creating a new user will be displayed.
     */
    public void loadUserObject() {
        try {
            if (!Objects.equals(this.userId, 0)) {
                setUserObject(this.serviceManager.getUserService().getById(this.userId));
            }
        } catch (DAOException e) {
            Helper.setFehlerMeldung("Error retrieving user with ID '" + this.userId + "'; ", e.getMessage());
        }
    }

    public int getUserId() {
        return this.userId;
    }

    public void setUserId(int id) {
        this.userId = id;
    }

    /**
     * Return list of projects.
     *
     * @return list of projects
     */
    public List<ProjectDTO> getProjects() {
        try {
            return serviceManager.getProjectService().findAll(true);
        } catch (DataException e) {
            logger.error("Unable to load projects: " + e.getMessage());
            return new LinkedList<>();
        }
    }

    /**
     * Return list of user groups.
     *
     * @return list of user groups
     */
    public List<UserGroupDTO> getUserGroups() {
        try {
            return serviceManager.getUserGroupService().findAll();
        } catch (DataException e) {
            logger.error("Unable to load user groups: " + e.getMessage());
            return new LinkedList<>();
        }
    }

    /**
     * Gets password.
     *
     * @return The password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets password.
     *
     * @param password
     *            The password.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    // TODO:
    // replace calls to this function with "/pages/userEdit" once we have
    // completely switched to the new frontend pages
    private String redirectToEdit(String urlSuffix) {
        String referrer = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap()
                .get("referer");
        String callerViewId = referrer.substring(referrer.lastIndexOf("/") + 1);
        if (!callerViewId.isEmpty() && callerViewId.contains("users.jsf")) {
            return "/pages/userEdit" + urlSuffix;
        } else {
            return "/pages/BenutzerBearbeiten" + urlSuffix;
        }
    }

    // TODO:
    // replace calls to this function with "/pages/users" once we have completely
    // switched to the new frontend pages
    private String redirectToList(String urlSuffix) {
        String referrer = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap()
                .get("referer");
        String callerViewId = referrer.substring(referrer.lastIndexOf("/") + 1);
        if (!callerViewId.isEmpty() && callerViewId.contains("userEdit.jsf")) {
            return "/pages/users" + urlSuffix;
        } else {
            return "/pages/BenutzerAlle" + urlSuffix;
        }
    }
}
