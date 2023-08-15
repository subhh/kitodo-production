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

package org.kitodo.api.dataformat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The administrative structure of the product of an element that passes through
 * a Production workflow.
 */
public class Workpiece {
    /**
     * The time this file was first created.
     */
    private GregorianCalendar creationDate = new GregorianCalendar();

    /**
     * The processing history.
     */
    private final List<ProcessingNote> editHistory = new ArrayList<>();

    /**
     * The identifier of the workpiece.
     */
    private String id;

    /**
     * The physical division that belongs to this workpiece. The physical division can have
     * children, such as a bound book that can have pages.
     */
    private PhysicalDivision physicalStructure = new PhysicalDivision();

    /**
     * The logical logical division.
     */
    private LogicalDivision logicalStructure = new LogicalDivision();

    /**
     * Returns the creation date of the workpiece.
     *
     * @return the creation date
     */
    public GregorianCalendar getCreationDate() {
        return creationDate;
    }

    /**
     * Sets the creation date of the workpiece.
     *
     * @param creationDate
     *            creation date to set
     */
    public void setCreationDate(GregorianCalendar creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Returns the edit history.
     *
     * @return the edit history
     */
    public List<ProcessingNote> getEditHistory() {
        return editHistory;
    }

    /**
     * Returns the ID of the workpiece.
     *
     * @return the ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID of the workpiece.
     *
     * @param id
     *            ID to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the physical structure of this workpiece.
     *
     * @return the physical structure
     */
    public PhysicalDivision getPhysicalStructure() {
        return physicalStructure;
    }

    /**
     * Returns the root element of the logical division.
     *
     * @return root element of the logical division
     */
    public LogicalDivision getLogicalStructure() {
        return logicalStructure;
    }

    /**
     * Sets the physical structure of the workpiece.
     *
     * @param physicalStructure
     *            physical structure to set
     */
    public void setPhysicalStructure(PhysicalDivision physicalStructure) {
        this.physicalStructure = physicalStructure;
    }

    /**
     * Sets the logical structure of the workpiece.
     *
     * @param logicalStructure
     *            logical structure to set
     */
    public void setLogicalStructure(LogicalDivision logicalStructure) {
        this.logicalStructure = logicalStructure;
    }

    @Override
    public String toString() {
        return id + ", " + logicalStructure;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;
        hashCode = prime * hashCode + ((id == null) ? 0 : id.hashCode());
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Workpiece workpiece = (Workpiece) o;
        return Objects.equals(creationDate, workpiece.creationDate)
                && Objects.equals(editHistory, workpiece.editHistory)
                && Objects.equals(id, workpiece.id)
                && Objects.equals(physicalStructure, workpiece.physicalStructure)
                && Objects.equals(logicalStructure, workpiece.logicalStructure);
    }

    /**
     * Returns all logical divisions of the logical structure of the
     * workpiece as a flat list. The list isn’t backed by the included
     * structural elements, which means that insertions and deletions in the
     * list would not change the logical divisions. Therefore, a list
     * that cannot be modified is returned.
     *
     * @return all logical divisions as an unmodifiable list
     */
    public List<LogicalDivision> getAllLogicalDivisions() {
        return treeStream(logicalStructure).collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns all child physical divisions of the physical division of the workpiece with
     * type "page" sorted by their {@code order} as a flat list. The root media
     * unit is not contained. The list isn’t backed by the physical divisions, which
     * means that insertions and deletions in the list would not change the
     * physical divisions. Therefore, a list that cannot be modified is returned.
     *
     * @return all physical divisions with type "page", sorted by their {@code order}
     */
    public List<PhysicalDivision> getAllPhysicalDivisionChildrenFilteredByTypePageAndSorted() {
        return getAllPhysicalDivisionChildrenFilteredByTypes(Collections.singletonList(PhysicalDivision.TYPE_PAGE));
    }

    /**
     * Returns all child physical divisions of the physical division of the workpiece with any of the types in the
     * provided list "types".
     *
     * @param types list of types
     * @return child physical division of given types
     */
    public List<PhysicalDivision> getAllPhysicalDivisionChildrenFilteredByTypes(List<String> types) {
        return physicalStructure.getChildren().stream()
                .flatMap(Workpiece::treeStream)
                .filter(physicalDivisionToCheck -> types.contains(physicalDivisionToCheck.getType()))
                .sorted(Comparator.comparing(PhysicalDivision::getOrder)).collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns total number of all physical divisions children of the physical division of the workpiece with types
     * from the given types list.
     * @param types list of physical division types as a List of String
     * @return the total number of physical divisions with given types.
     */
    public int getNumberOfAllPhysicalDivisionChildrenFilteredByTypes(List<String> types) {
        return Math.toIntExact(physicalStructure.getChildren().stream()
                .flatMap(Workpiece::treeStream)
                .filter(physicalDivisionToCheck -> types.contains(physicalDivisionToCheck.getType())).count());
    }

    /**
     * Returns all physical divisions of the physical division of the workpiece as a flat
     * list. The list isn’t backed by the physical divisions, which means that
     * insertions and deletions in the list would not change the physical divisions.
     * Therefore, a list that cannot be modified is returned.
     *
     * @return all physical divisions as an unmodifiable list
     */
    public List<PhysicalDivision> getAllPhysicalDivisions() {
        return treeStream(physicalStructure).collect(Collectors.toUnmodifiableList());
    }

    /**
     * Generates a stream of nodes from structure tree.
     *
     * @param tree
     *            starting node
     * @return all nodes as stream
     */
    @SuppressWarnings("unchecked")
    public static <T extends Division<T>> Stream<T> treeStream(Division<T> tree) {
        return Stream.concat(Stream.of((T) tree), tree.getChildren().stream().flatMap(Workpiece::treeStream));
    }
}
