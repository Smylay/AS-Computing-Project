package com.smylay.hr;

import com.smylay.hr.model.Absence;
import com.smylay.hr.model.Reason;
import java.io.IOException;
import java.io.Serializable;
import javax.ejb.Stateful;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import org.jboss.logging.Logger;
import org.joda.time.DateTime;
import org.primefaces.event.timeline.TimelineSelectEvent;
import org.primefaces.model.timeline.TimelineEvent;

// The absence request manager handles the whole procces of creating to approving a request made by the user.
// The following annotations are required to access the bean from the web pages that use it.
// "Stateful" means the bean holds values that persist across multiple web pages.
// "SessionScoped" details how long the state is saved. In this case the whole session.
// "Named" enables beans to be injected into each other which allows them to communicate between each other.
@Stateful
@SessionScoped
@Named
public class AbsenceRequestManager implements Serializable {
// Here I inject different beans. DBFacade handles database interaction. LoginManager handles the logged in user. Logger enables you to output messages to the screen.

    @Inject
    DBFacade dbFacade;
    @Inject
    LoginManager loginManager;
    @Inject
    Logger log;
    private Absence newAbsence;
    private Absence selectedAbsence;

// init creates a new absence, sets the Employeeid to that of the logged in user, and sets its approved boolean to false.
// "PostConstruct" means that this method runs straight after the construction of an instance of this class.
    @PostConstruct
    public void init() {
        newAbsence = new Absence();
        newAbsence.setEmployeeid(loginManager.getLoggedInUser());
        newAbsence.setApproved(false);
    }

    /**
     * This method accesses the database to retrieve a list of all possible reasons for absence. e.g. holiday.
     *
     * @param none
     *
     * @return List of Reasons
     */
    public List<Reason> getPossibleReasons() {
        return dbFacade.getAllReasons();
    }

    /**
     * This method called when the user clicks submit on the request absence form.
     *
     * @param none
     *
     * @return none
     */
    public void submitClicked() {
        // First we calculate the number of days difference between start date and end date and set that value to be associated with the new absence.
        int numDays = Utils.numDaysDifference(newAbsence.getStartdate(), newAbsence.getEnddate(), dbFacade);
        newAbsence.setNumdays(numDays);

        // saves the new absence to the datasbase.
        dbFacade.saveAbsence(newAbsence);
    }

    /**
     * This method called when the user clicks on a request on the timeline on the dashboard.
     *
     * @param the timeline event that wass clicked on.
     *
     * @return none
     */
    public void onSelect(TimelineSelectEvent e) {
        // This code retrieves the absence from the event that was clicked on the timeline.
        // The absence is stored in the data property of the timeline event when the timeline event was created.
        TimelineEvent timelineEvent = e.getTimelineEvent();
        selectedAbsence = (Absence) timelineEvent.getData();

        //redirect to approval page
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        try {
            context.redirect(context.getRequestContextPath() + "/absence-approval.xhtml?faces-redirect=true");
        } catch (IOException ex) {
            log.error("Error redirectinbg");
        }
    }

    /**
     * This method called when the system is working out how many absences a user has had in the current year for the
     * absence request page.
     *
     * @param none
     *
     * @return The number of absences had in the current year.
     */
    public Integer getAbsenceHad() {
        // This method creates two variables that work out the start date and end date of the year.
        DateTime startdate = new DateTime(selectedAbsence.getStartdate()).withDayOfYear(1).withTimeAtStartOfDay();
        DateTime enddate = startdate.plusYears(1);

        // This calls a database method to count and return the number of days holiday the user has had that year
        return dbFacade.getAbsenceHad(selectedAbsence.getEmployeeid(), startdate.toDate(), enddate.toDate(), 1);
    }

    /**
     * This method called when the user clicks approve on the absence approval page.
     *
     * @param none
     *
     * @return none
     */
    public void approveClicked() {
        // The absence aprroval boolean is set to true, the absence is saved.
        selectedAbsence.setApproved(true);
        dbFacade.saveAbsence(selectedAbsence);

        // Emails the user to tell them that their request has been approved.
        MailSenderBean.sendApprovedRequestMailer(selectedAbsence.getEmployeeid(), selectedAbsence);
    }

    /**
     * This method called when the user clicks deny on the absence approval page.
     *
     * @param none
     *
     * @return none
     */
    public void denyClicked() {
        // Deletes the absence from the database
        dbFacade.deleteAbsence(selectedAbsence);

        // emails the user to inform them that their request has been denied.
        MailSenderBean.sendApprovedRequestMailer(selectedAbsence.getEmployeeid(), selectedAbsence);
    }

    /**
     * This method is called when the user clicks cancel on the absence approval page.
     *
     * @param none
     *
     * @return none
     */
    public void cancelClicked() {
        // Deleteds the absence from the database.
        dbFacade.deleteAbsence(selectedAbsence);
    }

    /**
     * This method called when the system is working out whether to show the approval buttons to a user on the absence
     * approval page.
     *
     * @param none
     *
     * @return a boolean telling the web page whether to show the approval buttons or not.
     */
    public Boolean getShowApprovalButtons() {
        // First checks whether the logged in user is a manager or a superuser (only these roles can approve requests).
        if (loginManager.getLoggedInUser().getRoleid().getRoleid() == Constants.ROLE_MANAGER_ID || loginManager.getLoggedInUser().getRoleid().getRoleid() == Constants.ROLE_SUPERUSER_ID) {
            // Then, checks that the request isnt already approved).
            if (!selectedAbsence.getApproved()) {
                //Finally checks that the  reaason isnt sickness (sickness does not need approval).
                if (!selectedAbsence.getReasonid().getReasonid().equals(Constants.REASON_SICKNESS_ID)) {
                    // At this point the approval buttons can be shown.
                    return true;
                }
            }
        }

        // Do not show the approval buttons.
        return false;
    }

    /**
     * This method called when the system is working out whether to show the cancel buttons to a user on the absence
     * approval page.
     *
     * @param none
     *
     * @return a boolean telling the web page whether to show the approval buttons or not.
     */
    public Boolean getShowCancelButton() {
        // First checks if the logged in user is a manager or a superuser (these roles can always cancel a request).
        if (loginManager.getLoggedInUser().getRoleid().getRoleid() == Constants.ROLE_MANAGER_ID
                || loginManager.getLoggedInUser().getRoleid().getRoleid() == Constants.ROLE_SUPERUSER_ID) {
            return true;
        }

        // Then checks if the logged in user is the user that created the absence.
        if (loginManager.getLoggedInUser().getEmployeeid().equals(selectedAbsence.getEmployeeid().getEmployeeid())) {

            //Checks that the absence is not sickness (sickness cannot be cancelled).
            if (!selectedAbsence.getReasonid().getReasonid().equals(Constants.REASON_SICKNESS_ID)) {

                // Checks that the start date is in the future.
                if (selectedAbsence.getStartdate().after(new Date())) {
                    // Shows the cancel button.
                    return true;
                }
            }
        }

        //Does not show the cancel button.
        return false;
    }

    /**
     * This method calculates the Bradford factor of the user (https://en.wikipedia.org/wiki/Bradford_Factor).
     *
     * @param a = number of absences had in the current year.
     * @param d = the total number of days spent absent.
     *
     * @return b = Bradford factor.
     */
    public Integer calculateAbsenceRating(int s, int d) {
        int b = s * s * d;
        return b;
    }

//////// Getters and Setters ////////
    /**
     * @return the selectedAbsence
     */
    public Absence getSelectedAbsence() {
        return selectedAbsence;
    }

    /**
     * @param selectedAbsence the selectedAbsence to set
     */
    public void setSelectedAbsence(Absence selectedAbsence) {
        this.selectedAbsence = selectedAbsence;
    }

    /**
     * @return the newAbsence
     */
    public Absence getNewAbsence() {
        return newAbsence;
    }

    /**
     * @param newAbsence the newAbsence to set
     */
    public void setNewAbsence(Absence newAbsence) {
        this.newAbsence = newAbsence;
    }
}
