function Home() {
	BaseTemplatedWidget.call(this);
	this.title = "Home";
	
	var thiz = this;

	// Multi User
	thiz.bind("click", function() {
		thiz.serviceHandler(thiz.muStartBtn);
    }, thiz.muStartBtn);
	
	thiz.bind("click", function() {
		thiz.serviceHandler(thiz.muStopBtn);
    }, thiz.muStopBtn);
	
	thiz.bind("click", function() {
		thiz.serviceHandler(thiz.muRestartBtn);
    }, thiz.muRestartBtn);
	
	// Server
	thiz.bind("click", function() {
		thiz.serviceHandler(thiz.svShutdownBtn);
    }, thiz.svShutdownBtn);
	
	thiz.bind("click", function() {
		thiz.serviceHandler(thiz.svRestartBtn);
    }, thiz.svRestartBtn);
	
	setTimeout(function() {
		thiz.checkMuStatus();
	}, 100);
	
}
__extend(BaseTemplatedWidget, Home);

Home.prototype.onAttached = function() {
	
}

Home.prototype.getServiceName = function(node) {
	var parent = Dom.findParentWithAttribute(node, "serviceName");
	return parent.getAttribute("serviceName");
}

Home.prototype.serviceHandler = function(node) {
	var thiz = this;
	
	serviceName = thiz.getServiceName(node);
	action = node.innerHTML;
	var promptDialog = new PromptDialog(action, serviceName);
	promptDialog.open();
	
}

Home.prototype.checkMuStatus = function() {
	var thiz = this;
	
	$serviceController.isMuRunning(function(running) {
		if (running) {
			thiz.multiUser.innerText = "Multi User - RUNNING";
		} else {
			thiz.multiUser.innerText= "Multi User - STOPPED";
		}
				
    }.bind(thiz));
}

