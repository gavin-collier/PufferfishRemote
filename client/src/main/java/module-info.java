module com.pufferfish {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.sql;
    requires socket.io.client;
    requires engine.io.client;
    requires okhttp3;
    requires bluecove;
    opens com.pufferfish to javafx.fxml;
    exports com.pufferfish;
}
