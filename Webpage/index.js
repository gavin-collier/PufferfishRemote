const express = require('express');
const app = express();
const http = require('http');
const server = http.createServer(app);
const { Server } = require("socket.io");
const io = new Server(server);

app.use(express.static('public'))

app.get('/', (req, res) => {
  res.sendFile(__dirname + '/index.html');
});

app.get('/joinRoom', (req, res) => {
  res.sendFile(__dirname + '/joinRoom.html');
})

io.on('connection', (socket) => {
    console.log('a user connected to defaut hub');
    socket.on('disconnect', () => {
      console.log('user disconnected');
    });
    socket.on("changeRoom", (room) => {
      socket.room = room;
      socket.leaveAll();
      socket.join(room);
      getRoomID = Array.from(socket.rooms)[1];
      console.log("user [" + socket.id + "] joined room ID: " + room);
      socket.in(room).emit("newUserNotification", socket.id);
  });
});

server.listen(3000, () => {
  console.log('listening on *:3000');
});