module com.pufferfish {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires socket.io.client;
    requires engine.io.client;
    opens com.pufferfish to javafx.fxml;
    exports com.pufferfish;
}
