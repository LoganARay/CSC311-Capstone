package edu.farmingdale.csc311capstone.dashboard;

public abstract class PageController {
    protected ArcadeShellController shell;

    public final void attach(ArcadeShellController shell) {
        this.shell = shell;
        onShown();
    }

    protected abstract void onShown();

    public void onHidden() {}
}
