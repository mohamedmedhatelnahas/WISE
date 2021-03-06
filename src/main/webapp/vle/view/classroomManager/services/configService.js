angular.module('ConfigService', [])

.service('ConfigService', function($http, $q) {
	this.config = null;
	
	this.retrieveConfig = function() {
		var parentHref = parent.window.location.href;
		var runId = parent.window.location.search.split('runId=')[1].substring(0, parent.window.location.search.split('runId=')[1].indexOf('&'));
		
		return $http.get('http://localhost:8080/wise/request/info.html?action=getVLEConfig&runId=' + runId + '&gradingType=classroomManager&requester=grading&getRevisions=null').then(angular.bind(this, function(result) {
			var config = result.data;
			this.config = config;
			return config;
		}));
	};
	
	this.getConfig = function() {
		return this.config;
	};

	this.getConfigParam = function(urlName) {
		var value = null;
		
		if(this.config != null) {
			value = this.config[urlName];
		}
		
		return value;
	} 
	
	this.getRunId = angular.bind(this, function() {
		return this.getConfigParam('runId');
	});
});