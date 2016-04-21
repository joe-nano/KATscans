package no.uib.inf252.katscan.model;

import javax.swing.tree.MutableTreeNode;

/**
 *
 * @author Marcelo Lima
 */
public abstract class SubGroup extends Displayable {

    @Override
    public Displayable getParent() {
        return (Displayable) super.getParent();
    }

    @Override
    public void setParent(MutableTreeNode newParent) {
        if (newParent instanceof Displayable) {
            super.setParent(newParent);
        } else {
            throw new IllegalArgumentException("Can only have " + Displayable.class.getSimpleName() + " nodes as parents of " + getClass().getSimpleName() + " nodes.");
        }
    }

    @Override
    public void insert(MutableTreeNode child, int index) {
        if (child instanceof Project || child instanceof DataFile) {
            throw new IllegalArgumentException("Cannot add " + DataFile.class.getSimpleName() + " nodes or " + Project.class.getSimpleName() + "nodes to " + getClass().getSimpleName() + " nodes.");
        }
        super.insert(child, index);
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

}