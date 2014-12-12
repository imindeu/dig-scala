var eventData = function(userEmail, onError, onMessage, onOnline, onOffline){
    if("WebSocket" in window)
    {
        if(typeof onOnline !== "function") onOnline = function(){console.log('on');};
        if(typeof onOffline !== "function") onOffline = function(){console.log('off');};
        var socket = {
            wsUri: "ws://localhost:9000/out/websocket/"+userEmail,
            isConnected: false,
            websocket: null
        };
        var connect = function(){
            if(!this.isConnected)
            {
                if(this.websocket != null) this.websocket.close();
                this.websocket = new WebSocket(this.wsUri);
                this.websocket.onopen = function(evt){
                    if(!this.isConnected)
                    {
                        this.isConnected = true;
                        onOnline();
                    }
                }.bind(this);
                this.websocket.onclose = function(evt){
                    if(this.isConnected)
                    {
                        this.isConnected = false;
                        onOffline();
                        window.setInterval(connect.bind(socket), 3000);
                    }
                }.bind(this);
                this.websocket.onmessage = function(evt){
                    var data = JSON.parse(evt.data);
                    if(!data || !data.hasOwnProperty('key') || !data.hasOwnProperty('value'))
                    {
                        onError("Invalid message format!");
                    }else{
                        onMessage(data);
                    }
                };
                this.websocket.onerror = function(evt){
                    console.log(evt);
                    onError("Connection error!");
                };
            }
        }.bind(socket);
        connect();
    }else onError("WebSocket not supported!");
};
