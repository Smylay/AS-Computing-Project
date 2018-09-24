package com.smylay.hr;

import com.smylay.hr.model.Absence;
import javax.ejb.Stateful;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import java.util.Date;
import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import org.jboss.logging.Logger;
import org.joda.time.DateTime;
import org.primefaces.model.chart.MeterGaugeChartModel;
import org.primefaces.event.timeline.TimelineSelectEvent;
import org.primefaces.model.timeline.TimelineEvent;
import org.primefaces.model.timeline.TimelineModel;

// The dashboard manager handeles all functions of the dashboard.
// The following annotations are required to access the bean from the web pages that use it.
// "Stateful" means the bean holds values that persist across multiple web pages.
// "SessionScoped" details how long the state is saved. In this case the whole session.
// "Named" enables beans to be injected into each other which allows them to communicate between each other.
@SessionScoped
@Named
@Stateful
public class DashboardManager implements Serializable {
// Here I inject different beans. DBFacade handles database interaction. LoginManager handles the logged in user. 
// Logger enables you to output messages to the screen. AbsenceRequestManager handeles absence requests.

    @Inject
    DBFacade dbFacade;
    @Inject
    LoginManager loginManager;
    @Inject
    AbsenceRequestManager absenceRequestManager;
    @Inject
    Logger log;

    private TimelineModel model;
    private MeterGaugeChartModel meterGaugeModel;
    private Date start;
    private Date end;

    // "PostConstruct" means that this method runs straight after the construction of an instance of this class.
    // init calls createMeterGaugeModels, then sets the start date to the first day of the current month and the end
    // date to three months later. IT then calls timelineInit.
    @PostConstruct
    public void init() {
        createMeterGaugeModels();
        DateTime startDateTime = new DateTime();
        DateTime endDateTime = new DateTime();
        startDateTime = startDateTime.withDayOfMonth(1).withTimeAtStartOfDay();
        endDateTime = startDateTime.plusMonths(3);
        start = startDateTime.toDate();
        end = endDateTime.toDate();
        timelineInit();
    }

    /**
     * This method sets the visual qualities of the meter gauge.
     *
     * @param none
     *
     * @return none
     */
    private void createMeterGaugeModels() {
        meterGaugeModel = initMeterGaugeModel();
        meterGaugeModel.setTitle("Absence rating");
        meterGaugeModel.setSeriesColors("1af21a,81f22b,f2f22b,f2b02b,f2222b");
        meterGaugeModel.setGaugeLabel(loginManager.getLoggedInUser().getAbsencerating() + " Points");
        meterGaugeModel.setGaugeLabelPosition("bottom");
        meterGaugeModel.setShowTickLabels(true);
        meterGaugeModel.setLabelHeightAdjust(0);
        meterGaugeModel.setIntervalOuterRadius(102);
    }

    /**
     * This method calculates the model that the meter gauge will show.
     *
     * @param none
     *
     * @return meter gauge model.
     */
    private MeterGaugeChartModel initMeterGaugeModel() {
        // Works out the current year.
        DateTime startOfYear = new DateTime();
        DateTime endOfYear = new DateTime();
        startOfYear = startOfYear.withDayOfYear(1).withTimeAtStartOfDay();
        endOfYear = startOfYear.plusYears(1);
        Date yearStart = startOfYear.toDate();
        Date yearEnd = endOfYear.toDate();

        // Retrieves the number of individual absences and total number of days absent in the current year.
        int numOfAbsences = dbFacade.getNumOfAbsences(loginManager.getLoggedInUser(), yearStart, yearEnd, 2);
        int absenceHad = dbFacade.getAbsenceHad(loginManager.getLoggedInUser(), yearStart, yearEnd, 2);

        // Calulates the absence rating.
        int num = absenceRequestManager.calculateAbsenceRating(numOfAbsences, absenceHad);

        // Saves the absence rating to the Employee in the database.
        loginManager.getLoggedInUser().setAbsenceRating(num);
        dbFacade.saveEmployee(loginManager.getLoggedInUser());

        // Outputs a line of text displaying the users absence rating
        log.info("Absence Rating = " + loginManager.getLoggedInUser().getAbsencerating());

        // Checks if the users absence rating is higher than 1000 and sets the intervals for the  colours on the meter gauge.
        Integer max = 1000;
        if (loginManager.getLoggedInUser().getAbsencerating() > 1000) {
            max = loginManager.getLoggedInUser().getAbsencerating();
        }
        List<Number> intervals = new ArrayList<Number>();
        intervals.add(201);
        intervals.add(401);
        intervals.add(601);
        intervals.add(801);
        // If the users absence rating is larger than 1000 the maximum value of the guage is set to the users absence rating.
        intervals.add(max);

        // returns the model for the meter gauge
        return new MeterGaugeChartModel(loginManager.getLoggedInUser().getAbsencerating(), intervals);
    }

    /**
     * This method initialises the timeline.
     *
     * @param none
     *
     * @return none
     */
    protected void timelineInit() {
        model = new TimelineModel();

        // Gets a list of all the absences in the database.
        List<Absence> absences = dbFacade.getAllAbsences();
        for (Absence thisAbsence : absences) {
            String styleClass;
            // Sets the colour of the event on the timeline
            // Checks each absence's status. if it has been approved. its style class is set to approved (coloured green).
            if (thisAbsence.getApproved()) {
                styleClass = "approved";
            } else if (thisAbsence.getReasonid().getReasonid().equals(Constants.REASON_SICKNESS_ID)) {
                // if it has the reason sickness then its styleclass is set to sickness (coloured red).
                styleClass = "sickness";
            } else {
                // If it is neither of the above then the style class is set to requested (coloured orange). 
                styleClass = "requested";
            }
            // Adds 1 day to the length of the bar as you want the end date to be included as a day off.
            DateTime modifiedEndDate = new DateTime(thisAbsence.getEnddate());
            modifiedEndDate = modifiedEndDate.plusDays(1);
            TimelineEvent event = new TimelineEvent(thisAbsence, thisAbsence.getStartdate(), modifiedEndDate.toDate(), false, thisAbsence.getEmployeeid().getName(), styleClass);
            model.add(event);
        }

    }

    /**
     * This method is called when a timeline event is clicked on.
     *
     * @param e the event that was selected
     *
     * @return none
     */
    public void onSelect(TimelineSelectEvent e) {
        TimelineEvent timelineEvent = e.getTimelineEvent();

        // For debugging use. allows me to see which event was selected.
        FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Selected event:", timelineEvent.getData().toString());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    //////// Getters and Setters ////////
    /**
     * @return model the meter gauge model
     */
    public MeterGaugeChartModel getMeterGaugeModel() {
        return meterGaugeModel;
    }

    /**
     * @return model the timeline model to get
     */
    public TimelineModel getModel() {
        return model;
    }

    /**
     * @return the start
     */
    public Date getStart() {
        return start;
    }

    /**
     * @param start the start to set
     */
    public void setStart(Date start) {
        this.start = start;
    }

    /**
     * @return the end
     */
    public Date getEnd() {
        return end;
    }

    /**
     * @param end the end to set
     */
    public void setEnd(Date end) {
        this.end = end;
    }

    /**
     * @param meterGaugeModel the meterGaugeModel to set
     */
    public void setMeterGaugeModel(MeterGaugeChartModel meterGaugeModel) {
        this.meterGaugeModel = meterGaugeModel;
    }

    /**
     * @param model the model to set
     */
    public void setModel(TimelineModel model) {
        this.model = model;
    }

}
