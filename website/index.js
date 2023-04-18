const express = require("express");
const app = express();
const http = require("http");
const server = http.createServer(app);
const { Server } = require("socket.io");
const io = new Server(server);

//functions
function removeItemOnce(arr, value) {
  var index = arr.indexOf(value);
  if (index > -1) {
    arr.splice(index, 1);
  }
  return arr;
}

let rooms = []; //create the list of open rooms
let roomHosts = []

app.use(express.static("public"));

//http page managment
app.get("/", (req, res) => {
  res.sendFile(__dirname + "/index.html");
});

app.get("/home", (req, res) => {
  res.sendFile(__dirname + "/index.html");
});

app.get("/about", (req, res) => {
  res.sendFile(__dirname + "/about.html");
});

app.get("/joinRoom", (req, res) => {
  res.sendFile(__dirname + "/joinRoom.html");
});

app.get("/404", (req, res) => {
  res.sendFile(__dirname + "/404.html");
});

app.get("/503", (req, res) => {
  res.sendFile(__dirname + "/503.html");
});

//socket controlls
io.on("connection", (socket) => {
  console.log("a user connected to defaut hub");
  socket.on("disconnect", () => {
    if (roomHosts.includes(socket.id)){
      console.log("a host disconnected");
      socket.in(rooms[roomHosts.indexOf(socket.id)]).emit("roomEnded");
      rooms = removeItemOnce(rooms, rooms[roomHosts.indexOf(socket.id)]) //remove the room from the room list
      console.log("reaming rooms: " + rooms)
      roomHosts = removeItemOnce(roomHosts, socket.id); //remove the host from the host list
    }
    console.log("a user disconnected");
  });
  socket.on("createRoom", (room) => {
    socket.leaveAll();
    rooms.push(room) //add the room into the list of active rooms
    roomHosts.splice(rooms.indexOf(room), 0, socket.id); //mark the cliant that made the room as the host
    console.log("a user[" + socket.id + "] created a room: " + room);
    socket.join(room);
  })
  socket.on("checkRoom", (room, callback) => {
    callback = typeof callback == "function" ? callback : () => {};
    if (rooms.includes(room)) {
      try {
        callback("OK");
      } catch (error) {
        callback({ error: error.message });
      }
    } else {
      try {
        console.log("Failed to find room: " + room)
        callback("MISSING");
      } catch (error) {
        callback({ error: error.message });
      }
    }
  });
  socket.on("joinRoom", (room, callback) => {
    socket.leaveAll();
    if (rooms.includes(room)) {
      try {
        socket.join(room);
        console.log("user [" + socket.id + "] joined room ID: " + room);
        socket.in(room).emit("newUserNotification", socket.id);
        callback("OK");
      } catch (error) {
        callback({ error: error.message });
      }
    } else {
      try {
        callback("MISSING");
      } catch (error) {
        callback({ error: error.message });
      }
    }
  });
  socket.on("pressButton", (button) => {
    console.log("button pressed: " + button);
    socket.in(rooms[roomHosts.indexOf(socket.id)]).emit("buttonPressed", socket.id, button);
  });
});

//start the server
server.listen(8080, () => {
  console.log("listening on *:8080");
});

