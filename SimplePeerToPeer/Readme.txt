Enter class and line of code

Completed
1. (3 points) Send the message "Hello what’s up?" which will not work in the chat. Fix
that.

Completed  // in this case, the peer informs other peers and the leader that it has disconnected
2. (8 points) Check that if a non leader node (pawn) disconnects that it will be removed
correctly. If one peer detects another non leader node not responding, the peer
should inform the leader. The leader should then inform all other peers about the
not responding node. If the leader detects it, it will just inform the others to remove
this node.


3. (5 points) Make sure a peer is not entered twice into the list of peers.