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

package org.kitodo;

import javax.faces.bean.SessionScoped;
import javax.inject.Named;

@Named("ExtendedForm")
@SessionScoped
public class ExtendedForm {


    public String value = "ExtendedForm";

    public ExtendedForm() {
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}