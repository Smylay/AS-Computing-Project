package com.smylay.hr;

import com.smylay.hr.model.Reason;
import java.io.Serializable;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.inject.Inject;
import javax.inject.Named;

// The reason converter was sourced from elsewhere and allows me to retrieve reasons from the database as an object or a string.
// The following annotations are required to access the bean from the web pages that use it.
// "Named" enables beans to be injected into each other which allows them to communicate between each other.
// "ApplicationScoped" details how long the state is saved. in this case for the duration that the application is running.
@Named
@ApplicationScoped
public class ReasonConverter implements Converter, Serializable {

    // Here I inject different beans. DBFacade handles database interaction.
    @Inject
    DBFacade dbFacade;

    private static final long serialVersionUID = 1L;

    @Override
    public Object getAsObject(FacesContext facesContext, UIComponent component, String submittedValue) {
        if (submittedValue.trim().equals("")) {
            return null;
        } else {
            try {
                int id = Integer.parseInt(submittedValue);
                Reason r = dbFacade.getEntity(Reason.class, id);
                return r;
            } catch (NumberFormatException ex) {

                throw new ConverterException(new FacesMessage(FacesMessage.SEVERITY_ERROR, "Conversion Error", "Not a valid id"));
            }
        }
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent component, Object value) {
        if (value == null) {

            return null;
        } else {
            if (value instanceof Reason) {
                return "" + ((Reason) value).getReasonid();
            } else {
                return "" + value.toString();
            }
        }
    }
}
