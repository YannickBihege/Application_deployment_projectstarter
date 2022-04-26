module com.udacity.catpoint.image {
    exports com.udacity.catpoint.image.service to com.udacity.catpoint.security; // accessible to this specific module
    requires java.desktop;
    requires org.slf4j;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.services.rekognition;
}