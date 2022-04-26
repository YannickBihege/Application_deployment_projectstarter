module com.udacity.catpoint.security {
    requires com.udacity.catpoint.image;
    requires miglayout;
    requires java.desktop;
    requires java.prefs;
    requires com.google.gson;
    requires java.sql;
    requires guava;
    opens com.udacity.catpoint.security.data to com.google.gson;
}