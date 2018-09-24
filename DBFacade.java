package com.smylay.hr;

import com.smylay.hr.model.Absence;
import com.smylay.hr.model.Employee;
import com.smylay.hr.model.Holidays;
import com.smylay.hr.model.Reason;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateful;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.jboss.logging.Logger;

// DBFacade handles all database interaction.
// The following annotations are required to access the bean from the web pages that use it.
// "Stateful" means the bean holds values that persist across multiple web pages.
// "Named" enables beans to be injected into each other which allows them to communicate between each other.
// "SessionScoped" details how long the state is saved. In this case the whole session.
@Named
@Stateful
@SessionScoped
public class DBFacade implements Serializable {

    // "PersistenceContext" declares the entity manager that manages reading and saving entities to the database.
    @PersistenceContext(unitName = "searchpointHRPU")
    EntityManager em;

    // Here I inject different beans. Logger enables you to output messages to the screen.
    @Inject
    Logger log;

    /**
     * Gets a user from the database given their username and password
     *
     * @param username
     * @param password
     *
     * @return an employee entity.
     */
    public Employee getUserFromDatabase(String username, String password) {
        // Create the hibernate query language (HQL) query.
        String hql = "select Object(e) from Employee e where e.username = :username and e.password = :password";
        Query qry = em.createQuery(hql);

        // Setting the username and password parameters of the query.
        qry.setParameter("username", username);
        qry.setParameter("password", password);

        try {
            // Executes the query and returns a employee (if found).
            Employee retVal = (Employee) qry.getSingleResult();
            return retVal;

        } catch (Exception ex) {
            // An exception is thrown if no em,ployee is found (returns null).
            return null;
        }
    }

    /**
     * Generic method to return any entity from the database
     *
     * @param type = class of entity.
     * @param id = the id of the entity.
     *
     * @return an entity of the given type.
     */
    public <T extends Object> T getEntity(Class<T> type, Integer id) {
        try {
            if (id == null) {
                log.warn("getEntity: ID is NULL");
                return null;
            }
            return em.find(type, id);
        } catch (Exception e) {
            log.warn("getEntity: Exception: no Entity found for ID : " + id);
            return null;
        }
    }

    /**
     * Saves an employee entity to the database.
     *
     * @param employee the employee to save
     *
     * @return none
     */
    public void saveEmployee(Employee employee) {
        // check to see if the employee already exists in the database (if it already exists it will have an ID).
        if (employee.getEmployeeid() == null) {

            // If the employee does not already exist, we create a new employee in the database by calling persist.
            em.persist(employee);
        } else {

            // If the employee does already exist, we save the changes made to the employee in the database by calling merge.
            em.merge(employee);
        }
    }

    /**
     * Saves an absence entity to the database.
     *
     * @param absence he absence to save
     *
     * @return none
     */
    public void saveAbsence(Absence absence) {
        // check to see if the absence already exists in the database (if it already exists it will have an ID).
        if (absence.getAbsenceid() == null) {

            // If the absence does not already exist, we create a new absence in the database by calling persist.
            em.persist(absence);
        } else {

            // If the absence does already exist, we save the changes made to the absence in the database by calling merge.
            em.merge(absence);
        }
    }

    /**
     * Deletes an absence entity from the database.
     *
     * @param absence the absence to delete.
     *
     * @return none
     */
    public void deleteAbsence(Absence absence) {
        em.remove(getEntity(Absence.class, absence.getAbsenceid()));
    }

    /**
     * Returns a list of all reasons from the database.
     *
     * @param none
     *
     * @return List of reasons
     */
    public List<Reason> getAllReasons() {
        String hql = "SELECT object(r) FROM Reason r";
        Query qry = em.createQuery(hql);
        return qry.getResultList();
    }

    /**
     * Returns a list of all employees from the database.
     *
     * @param none
     *
     * @return List of employees
     */
    public List<Employee> getAllEmployees() {
        String hql = "SELECT object(e) FROM Employee e";
        Query qry = em.createQuery(hql);
        return qry.getResultList();
    }

    /**
     * Returns a list of all absences from the database.
     *
     * @param none
     *
     * @return List of absences
     */
    public List<Absence> getAllAbsences() {
        String hql = "SELECT object(a) FROM Absence a";
        Query qry = em.createQuery(hql);
        return qry.getResultList();
    }

    /**
     * Returns a list of all holidays from the database.
     *
     * @param none
     *
     * @return List of holidays
     */
    public List<Holidays> getAllHolidays() {
        String hql = "SELECT object(h) FROM Holidays h";
        Query qry = em.createQuery(hql);
        return qry.getResultList();
    }

    /**
     * Returns the number of days absent an employee has had between two dates from the database.
     *
     * @param employee = an employee
     * @param startdate = the start date of the period.
     * @param enddate = the end date of the period.
     * @param reasonId = the id of the reason for the absences.
     *
     * @return Total number of days absent.
     */
    public Integer getAbsenceHad(Employee employee, Date startdate, Date enddate, Integer reasonId) {

        // Create the hibernate query language (HQL) query.
        String sql = "SELECT SUM(a.numdays) FROM absence a "
                + "WHERE a.employeeid = :employeeid "
                + "AND ((a.enddate > :startdate AND a.enddate < :enddate) OR (a.startdate > :startdate AND a.startdate < :enddate)) "
                + "AND a.reasonid = :reasonId";
        Query qry = em.createNativeQuery(sql);

        // Setting the parameters of the query.
        qry.setParameter("startdate", startdate);
        qry.setParameter("enddate", enddate);
        qry.setParameter("employeeid", employee.getEmployeeid());
        qry.setParameter("reasonId", reasonId);
        try {

            // Execute the query and return the number of days spent absent.
            Double result = (Double) qry.getSingleResult();
            return result.intValue();
        } catch (Exception e) {

            // We could geet an exception if the employee has had no absences.
            return 0;
        }
    }

    /**
     * Returns the number of individual absences an employee has had between two dates from the database.
     *
     * @param employee = an employee
     * @param startdate = the start date of the period.
     * @param enddate = the end date of the period.
     * @param reasonId = the id of the reason for the absences.
     *
     * @return Total number of absences.
     */
    public Integer getNumOfAbsences(Employee employee, Date startdate, Date enddate, Integer reasonId) {

        // Create the hibernate query language (HQL) query.
        String sql = "SELECT COUNT(a.absenceid) FROM absence a "
                + "WHERE a.employeeid = :employeeid "
                + "AND ((a.enddate > :startdate AND a.enddate < :enddate) OR (a.startdate > :startdate AND a.startdate < :enddate)) "
                + "AND a.reasonid = :reasonId";

        // Setting the parameters of the query
        Query qry = em.createNativeQuery(sql);
        qry.setParameter("startdate", startdate);
        qry.setParameter("enddate", enddate);
        qry.setParameter("employeeid", employee.getEmployeeid());
        qry.setParameter("reasonId", reasonId);
        try {

            // Execute the query and return the number absences.
            BigInteger result = (BigInteger) qry.getSingleResult();
            return result.intValue();
        } catch (Exception e) {

            // We could geet an exception if the employee has had no absences (return 0).
            return 0;
        }
    }

}
