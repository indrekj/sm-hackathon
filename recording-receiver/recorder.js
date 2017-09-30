const fs = require('fs');
const WebSocket = require('ws');
const port = process.env['PORT'] || 9443;

const wss = new WebSocket.Server({port: port});

function broadcast(data) {
  wss.clients.forEach(function(client) {
    if (client.readyState === WebSocket.OPEN && client.isVisitor === true) {
      client.send(data, {binary: true});
    }
  });
}

wss.on('connection', (socket, req) => {
  socket.isVisitor = req.url == '/visitor';
  console.log(new Date(), 'got new websocket connection, visitor: ', socket.isVisitor);

  socket.on('message', (data) => {
    console.log(new Date(), 'received screenshot');
    broadcast(data);
  });
});


console.log(`WebSocket listening on port ${port}!`);
