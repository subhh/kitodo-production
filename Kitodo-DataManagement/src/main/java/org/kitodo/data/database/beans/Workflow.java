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

package org.kitodo.data.database.beans;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.kitodo.data.database.enums.WorkflowStatus;
import org.kitodo.data.database.persistence.WorkflowDAO;

@Entity
@Table(name = "workflow")
public class Workflow extends BaseIndexedBean {

    @Column(name = "title")
    private String title;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private WorkflowStatus status = WorkflowStatus.DRAFT;

    @ManyToOne
    @JoinColumn(name = "client_id", foreignKey = @ForeignKey(name = "FK_workflow_client_id"))
    private Client client;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Template> templates;

    /**
     * Empty constructor.
     */
    public Workflow() {
    }

    /**
     * Public constructor.
     *
     * @param title
     *            of workflow
     */
    public Workflow(String title) {
        this.title = title;
    }

    /**
     * Get title.
     *
     * @return value of title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set title.
     *
     * @param title
     *            as String
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Get status of the workflow.
     *
     * @return value of status
     */
    public WorkflowStatus getStatus() {
        return status;
    }

    /**
     * Set status of the workflow.
     *
     * @param status as org.kitodo.data.database.beans.Workflow.Status
     */
    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    /**
     * Get client.
     *
     * @return the client bean
     */
    public Client getClient() {
        return client;
    }

    /**
     * Set client.
     *
     * @param client
     *            bean
     */
    public void setClient(Client client) {
        this.client = client;
    }

    /**
     * Get list of template assigned to this workflow.
     *
     * @return list of template assigned to this workflow
     */
    public List<Template> getTemplates() {
        initialize(new WorkflowDAO(), this.templates);
        if (Objects.isNull(this.templates)) {
            this.templates = new ArrayList<>();
        }
        return this.templates;
    }

    /**
     * Set list of template assigned to this workflow.
     *
     * @param templates
     *            list of template assigned to this workflow
     */
    public void setTemplates(List<Template> templates) {
        this.templates = templates;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object instanceof Workflow) {
            Workflow workflow = (Workflow) object;
            return Objects.equals(this.getId(), workflow.getId());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, status);
    }
}
