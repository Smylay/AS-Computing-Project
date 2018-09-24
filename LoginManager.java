package com.smylay.hr;

import com.smylay.hr.model.Employee;
import java.io.IOException;
import java.io.Serializable;
import javax.ejb.Stateful;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import org.jboss.logging.Logger;

// The log-in manager handles the log in proccess and the logged in user.
// The following annotations are required to access the bean from the web pages that use it.
// "Stateful" means the bean holds values that persist across multiple web pages.
// "SessionScoped" details how long the state is saved. In this case the whole session.
// "Named" enables beans to be injected into each other which allows them to communicate between each other.
@Stateful
@SessionScoped
@Named
public class LoginManager implements Serializable {

    // Here I inject different beans. DBFacade handles database interaction. Logger enables you to output messages to the screen.
    @Inject
    DBFacade dbFacade;

    @Inject
    Logger log;

    private String username;
    private String password;
    private Boolean rememberMe;

    private Employee loggedInUser;

    /**
     * This method is called when login is clicked.
     *
     * @param none
     *
     * @return none
     */
    public void loginClicked() {
        loggedInUser = dbFacade.getUserFromDatabase(username, password);
        // Checks if the information the user entered is already stored in the database (if not getUserFromDatabase will return null).
        if (loggedInUser != null) {
            ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
            //redirects to the dashboard or displays an error if there is an exception.
            try {
                context.redirect(context.getRequestContextPath() + "/dashboard.xhtml?faces-redirect=true");
            } catch (IOException ex) {
                log.error("Error redirecting");
            }
        } else {
            // If the user is not found in the database the system outputs error messages.
            FacesMessage message = new FacesMessage();
            message.setSeverity(FacesMessage.SEVERITY_ERROR);
            message.setSummary("Login Failed! Please try again");
            message.setDetail("Login Failed! Please try again");
            FacesContext.getCurrentInstance().addMessage(null, message);
        }
    }

    /**
     * This method is called when logout is clicked. It sets the logged in user to null.
     *
     * @param none
     *
     * @return none
     */
    public void logoutClicked() {
        loggedInUser = null;
    }

    /**
     * @return boolean is there a user logged in?
     */
    public Boolean getLoggedIn() {
        if (loggedInUser != null) {
            return true;
        } else {
            return false;
        }
    }

    //////// Getters and Setters ////////
    
    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the loggedInUser
     */
    public Employee getLoggedInUser() {
        return loggedInUser;
    }

    /**
     * @param loggedInUser the loggedInUser to set
     */
    public void setLoggedInUser(Employee loggedInUser) {
        this.loggedInUser = loggedInUser;
    }

}
