package org.kitodo;

import javax.faces.bean.SessionScoped;
import javax.inject.Named;

/**
 * Created by julienambos on 22.09.17.
 */
@Named("ExtendedForm")
@SessionScoped
public class ExtendedForm {


    public String value = "567TEST";

    public ExtendedForm() {
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}