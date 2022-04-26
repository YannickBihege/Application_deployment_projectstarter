module com.udacity.catpoint.security {
    requires transitive  com.udacity.catpoint.image;
    requires miglayout;
    requires java.desktop;
    requires java.prefs;
    requires com.google.gson;
    requires java.sql;
    opens com.udacity.catpoint.security.data to com.google.gson;

}