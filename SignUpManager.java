package com.smylay.hr;

import com.smylay.hr.model.Employee;
import com.smylay.hr.model.Role;
import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.ejb.Stateful;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

// The sign-up manager handles the creation of a new user.
// The following annotations are required to access the bean from the web pages that use it.
// "Stateful" means the bean holds values that persist across multiple web pages.
// "SessionScoped" details how long the state is saved. In this case the whole session.
// "Named" enables beans to be injected into each other which allows them to communicate between each other.
@Stateful
@SessionScoped
@Named
public class SignUpManager implements Serializable {

    private Employee newEmployee;
    private String password;
    private String confirmPassword;
    private String nextPage;

    // Here I inject different beans. DBFacade handles database interaction.
    @Inject
    DBFacade dbFacade;

    // init creates a new employee, sets the role employee, and sets password and confirmPassword to null.
    // "PostConstruct" means that this method runs straight after the construction of an instance of this class.
    @PostConstruct
    public void init() {
        newEmployee = new Employee();
        Role role = (Role) dbFacade.getEntity(Role.class, Constants.ROLE_EMPLOYEE_ID);
        newEmployee.setRoleid(role);
        password = null;
        confirmPassword = null;
    }

    /**
     * This method is called when submit is clicked.
     *
     * @param none
     *
     * @return none
     */
    public void submitClicked() {
        // Checks if password matches confirmPassword
        if (password.equals(confirmPassword)) {
            // Sets various default values for a new employee, such as absence rating to zero. 
            newEmployee.setAbsenceRating(0);
            newEmployee.setDaysallowed(25);
            newEmployee.setNumOfAbsences(0);
            // sets the new employee's password to the password they entered and saves the employee to the database.
            newEmployee.setPassword(password);
            dbFacade.saveEmployee(newEmployee);
            // Redirects to the log-in screen.
            nextPage = "log-in-screen.xhtml?faces-redirect=true";
        } else {
            // If password and confirmPassword do not match then an error shows, explaining this.
            FacesMessage message = new FacesMessage();
            message.setSeverity(FacesMessage.SEVERITY_ERROR);
            message.setSummary("Passwords do not match");
            message.setDetail("Passwords do not match");
            FacesContext.getCurrentInstance().addMessage(null, message);
            nextPage = null;
        }

    }

    //////// Getters and Setters ////////
    /**
     * @return the newEmployee
     */
    public Employee getNewEmployee() {
        return newEmployee;
    }

    /**
     * @param newEmployee the newEmployee to set
     */
    public void setNewEmployee(Employee newEmployee) {
        this.newEmployee = newEmployee;
    }

    /**
     * @return the nextPage
     */
    public String getNextPage() {
        return nextPage;
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
     * @return the confirmPassword
     */
    public String getConfirmPassword() {
        return confirmPassword;
    }

    /**
     * @param confirmPassword the confirmPassword to set
     */
    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
