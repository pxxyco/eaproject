package it.unical.ea_project_javafx.controller.organizer;

/** Dedicated catalog view; shares only offer operations with the dashboard. */
public class EventsController extends OrganizerDashboardController {
    @Override
    protected boolean catalog() {
        return true;
    }
}
