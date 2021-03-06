/**
 * Functions specific to step authoring
 * 
 * @author patrick lawler
 * @author jonathan breitbart
 */

/**
 * Shows the author step dialog
 */
View.prototype.showAuthorStepDialog = function(){
	$('#authorStepDialog').show();
	$('#overlay').show();
};

/**
 * Hides the author step dialog
 */
View.prototype.hideAuthorStepDialog = function(){
	$('#authorStepDialog').hide();
	$('#overlay').hide();
};

/**
 * Sets the initial state of the authoring step dialog window
 */
View.prototype.setInitialAuthorStepState = function(){
	/*
	 * this.activeContent is the content object that contains the
	 * latest content that is being authored, this may contain
	 * content that is not yet saved to the server. this does not
	 * contain the injected contentBaseUrl.
	 * 
	 * this.preservedContentString is the content string that
	 * is currently saved to the server. this does not contain the
	 * injected contentBaseUrl.
	 * 
	 * this.activeNode.content is the content object that is used
	 * in the preview step. it currently does not contain the injected 
	 * contentBaseUrl but after previewStep() is called, the content
	 * object will contain the injected contentBaseUrl.
	 */
	// if this is a selfRendering step (like HTMLNode), there will be no advanced mode
	if (this.activeNode.selfRendering) {
		this.easyMode = true;		
		$('#easyFalse').attr('disabled','disabled');
	} else {
		$('#easyFalse').removeAttr('disabled');
	}
	
	/*
	 * set active content as copy of the active node's content.
	 * this will serve as a temporary buffer for the content
	 * that is being authored. 
	 */
	this.activeContent = createContent(this.activeNode.content.getContentUrl());
	
	/*
	 * obtain the content string that is currently saved to the server.
	 * whenever the authored content is saved to the server, this string
	 * will be updated so that it always mirrors the content saved on the
	 * server.
	 */
	this.preservedContentString = this.activeNode.content.getContentString();
	
	/* set step saved boolean */
	this.stepSaved = true;
	
	/* set radiobutton for easy/advanced mode */
	if(this.easyMode){
		document.getElementById('easyTrue').checked = true;
	} else {
		document.getElementById('easyFalse').checked = true;
	}
	
	/* set refresh as typing mode */
	document.getElementById('refreshCheck').checked = this.updateNow;
	
	/* generate the authoring */
	if(this.easyMode){
		this[this.resolveType(this.activeNode.type)].generatePage(this);

		this.insertCommonComponents();
	} else {
		this.generateAdvancedAuthoring();
	}
	
	/*
	 * clear out the preview frame, we do this so that the previous step the
	 * author previewed does not still show up if they are now authoring a
	 * note step
	 */
	$('#previewFrame').html('');
	
	if($('#notePanel').length > 0) {
		/*
		 * clear out the content in the notePanel. for some reason the content
		 * in the notePanel was preventing steps from being previewed after
		 * the author previewed a note. the content in the notePanel contained
		 * a base href which was later being used when retrieving step html files
		 * which caused the authoring tool to look for the step html files in the
		 * wrong path.
		 */
		$('#notePanel').empty();	
	}
	
	//clear out the previous nodeId value in the preview frame
	$('#previewFrame').nodeId = null;
	
	/* show in preview frame */
	this.previewStep();
};

/**
 * Returns the appropriate type for the purposes of authoring of the given node type.
 * In most cases, it is the node type itself. i.e. MatchSequenceNode = MatchSequenceNode,
 * but sometimes it's not: NoteNode = OpenResponseNode.
 */
View.prototype.resolveType = function(type){
	if(type=='NoteNode'){
		return 'OpenResponseNode';
	} else if(type=='ChallengeNode'){
		return 'MultipleChoiceNode';
	} else if(type=='BranchNode'){
		return 'MultipleChoiceNode';
	} else {
		return type;
	}
};

/**
 * Inserts the link div into the current step's authoring and processes existing links.
 */
View.prototype.insertLinkTo = function(){
	/* move the linkto into the prompt div for this node and show it */
	try{
		var linkDiv = document.getElementById('linkContainer').removeChild(document.getElementById('linkDiv'));
		document.getElementById('promptDiv').appendChild(linkDiv);
	} catch (e){/* do nothing */}
	
	/* process the links for this step */
	this.linkManager.processExistingLinks(this);
};


/**
 * Changes the boolean value of easyMode to that of the given value
 */
View.prototype.authorStepModeChanged = function(val){
	if(this.stepSaved || confirm('You are about to switch authoring mode but have not saved your changes. If you continue, your changes will be lost. Do you wish to continue?')){
		if(val=='true'){
			this.easyMode = true;
		} else {
			this.easyMode = false;
		}
		
		this.cleanupCommonComponents();
		this.setInitialAuthorStepState();
	} else {
		/* user canceled put selection back */
		if(this.easyMode){
			document.getElementById('easyTrue').checked = true;
		} else {
			document.getElementById('easyFalse').checked = true;
		}
	}
};

/**
 * Changes the boolean value of updateNow to that selected by the user in the document
 */
View.prototype.updateRefreshOption = function(){
	this.updateNow = document.getElementById('refreshCheck').checked;
};

/**
 * Changes the boolean value of updateNow to that selected by the user in the document
 */
View.prototype.setRefreshAsTyping = function(doRefreshAsTyping){
	console.log('setRefreshAsTyping');
	document.getElementById('refreshCheck').checked = doRefreshAsTyping;
	this.updateNow = doRefreshAsTyping;
}

/**
 * generates the text area to author content when in advanced authoring mode
 */
View.prototype.generateAdvancedAuthoring = function(){
	var parent = document.getElementById('dynamicParent');
	
	/* remove any existing elements */
	while(parent.firstChild){
		parent.removeChild(parent.firstChild);
	};
	
	/* create elements for authoring content */
	var pageDiv = createElement(document, 'div', {id:'dynamicPage', style:'width:100%;height:100%'});
	var ta = createElement(document, 'textarea', {id:'sourceTextArea', style:'width:100%;height:100%', onkeyup:'eventManager.fire("sourceUpdated")'});
	parent.appendChild(pageDiv);
	pageDiv.appendChild(ta);
	
	/* fill with active node's content string */
	ta.style.width = '100%';
	ta.style.height = '100%';
	ta.value = this.activeContent.getContentString();
};

/**
 * saves the currently open step's content and hides the authoring dialog.
 * saving is performed even if nothing has changed because we need to revert
 * the activeNode.content back to the content that does not contain the
 * injected contentBaseUrl.
 */
View.prototype.closeOnStepSaved = function(success){
	if(success || confirm('Save failed, do you still want to exit?')){
		this.cleanupCommonComponents();

		try {
			// remove any tinyMCE instances
			tinymce.remove();			
		} catch(e) {
			
		}
		
		//clear the authoring div
		document.getElementById('dynamicPage').innerHTML = '';

		this.hideAuthorStepDialog();
		
		/*
		 * the activeNode.content contains the injected contentBaseUrl content
		 * so we need to replace it with the content that does not have
		 * the injected contentBaseUrl
		 */
		if (this.activeNode) {
			this.activeNode.content.setContent(this.preservedContentString);
			this.activeNode.baseHtmlContent = undefined;
			this.activeNode = undefined;			
		}
		this.activeContent = undefined;
		this.activeTA = undefined;
		this.preservedContent = undefined;
		this.stepSaved = true;
	}
};

/**
 * Prompts user if they are trying to exit before saving and hides the authoring dialog if they wish to continue
 */
View.prototype.closeStep = function(){
	if(this.stepSaved || confirm('Changes have not been saved, do you still wish to exit?')){
		this.closeOnStepSaved(true);
	}
};

/**
 *  refreshes the preview 
 */
View.prototype.refreshNow = function(){
	this.sourceUpdated(true);
};

/**
 * saves the currently open step's content and calls individual step type's
 * save function so that any other tasks can be done at that time.
 * @param close
 * @param bypassUpdateSource boolean whether we want to skip updating the source
 */
View.prototype.saveStep = function(close, bypassUpdateSource){
	/* calls individual step type's save() if it exists */
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].save){
		this[this.resolveType(this.activeNode.type)].save(close);
	}
	
	/* only save activeNode content if it is not an html type or if we are in advanced mode */
	if(!this.activeNode.selfRendering || !this.easyMode){
		//check if we want to skip updating the source (in the case the source is already updated)
		if(!bypassUpdateSource) {
			/* 
			 * we will update the source
			 * we need to update the active content before saving if the user has switched off the refresh as typing
			 */
			this.sourceUpdated(true);
		}
		
		/* get json content as string */
		var contentString = encodeURIComponent($.stringify(this.activeContent.getContentJSON(),null,3));
		
		if(contentString == 'undefined') {
			//the JSON is invalid so we will not save
			alert('Error: JSON is invalid, unable to save step.');
		} else {
			//the JSON is valid so we will save
			
			/* success callback for updating content file on server */
			var success = function(txt,xml,obj){
				obj.stepSaved = true;
				obj.notificationManager.notify('Content saved to server.', 3);
				obj.eventManager.fire('setLastEdited');
				
				//update our local copy of the step content that mirrors the step content on the server
				obj.preservedContentString = obj.activeContent.getContentString();
				
				if(close){
					obj.eventManager.fire('closeOnStepSaved', [true]);
				}
			};
			
			/* failure callback for updating content file on server */
			var failure = function(o,obj){
				obj.notificationManager.notify('Warning: Unable to save content to server!', 3);
				if(close){
					obj.eventManager.fire('closeOnStepSaved', [false]);
				}
			};
			
			/* update content to server */
			this.connectionManager.request('POST', 3, this.requestUrl, {forward:'filemanager', projectId:this.portalProjectId, command:'updateFile', fileName:this.activeContent.getFilename(this.getProject().getContentBase()), data:contentString},success,this,failure);			
		}
	}
};

/**
 * Update content and reload preview when user changes the content
 */
View.prototype.sourceUpdated = function(now){
	if(this.updateNow || now){
		this.stepSaved = false;
		
		if(this.easyMode){
			/* have the step type authoring update the content */
			this[this.resolveType(this.activeNode.type)].updateContent();
		} else {
			/* update content from source text area */
			this.activeContent.setContent(document.getElementById('sourceTextArea').value);
		}
		
		this.previewStep();
	}
};

/**
 * Previews the activeNode's content in the preview window
 */
View.prototype.previewStep = function(){
	//get the active content
	var contentString = this.activeContent.getContentString();
	
	//inject the asset full asset path
	contentString = this.injectAssetPath(contentString);
	
	//create a new content object
	var contentObj = createContent(this.activeNode.getContent().getContentUrl());
	
	//set the content string of the new content object
	contentObj.setContent(contentString);
	
	//set the new content with the absolute asset paths into the active node 
	this.activeNode.content.setContent(contentObj.getContentJSON());
	
	/* we don't want broken preview steps to prevent the user from saving
	 * content so let's try to catch errors here */
	
	//for HtmlNode steps we need to inject the full asset path into the html content
	if(this.activeNode.type == 'HtmlNode') {
		//get the html content
		var htmlContent = this.activeNode.baseHtmlContent.getContentString();
		
		//inject the full asset path
		htmlContent = this.injectAssetPath(htmlContent);
		
		//set the updated html content back
		this.activeNode.baseHtmlContent.setContent(htmlContent);
	} else if (this.activeNode.type == 'WebAppNode') {
		//these nodes do not support step preview at the moment
		return;
	}
	
	try{
		/* render the node */
		this.activeNode.render(window.frames['previewFrame']);
	} catch(e){
		this.notificationManager.notify('Error generating preview for step authoring. The following error was generated: ' + e,1);
	}
};

/**
 * Replace all occurrences of "assets" with the full assets path
 * e.g.
 * "http://wise4.berkeley.edu/curriculum/135/assets"
 * @param contentString a string containing the content from a step
 * @return the content with all occurrences of "assets" replaced with
 * the full assets path
 */
View.prototype.injectAssetPath = function(contentString) {
	var contentBaseUrl = "";
	
	/*
	 * get the content base url which should be the url to the curriculum folder
	 * e.g.
	 * http://wise4.berkeley.edu/curriculum
	 */
	var contentBaseUrl = this.activeNode.getAuthoringModeContentBaseUrl();
	
	//if the contentBaseUrl ends with '/' we will remove it
	if(contentBaseUrl.charAt(contentBaseUrl.length - 1) == '/') {
		contentBaseUrl = contentBaseUrl.substring(0, contentBaseUrl.length - 1);
	}

	var fullProjectFolderPath = null;
	
	if(this.getProjectMetadata().projectFolder != null) {
		/*
		 * the project folder is in the project meta data
		 * e.g.
		 * /135
		 * 
		 * so the full project folder path will look like
		 * http://wise4.berkeley.edu/curriculum/135
		 */
		fullProjectFolderPath = contentBaseUrl + this.getProjectMetadata().projectFolder;
	}
	
	//make sure the projectFolder ends with '/'
	if(fullProjectFolderPath.charAt(fullProjectFolderPath.length - 1) != '/') {
		fullProjectFolderPath += '/';
	}
	
	/*
	 * replace any relative references to assets/ with the absolute path to the assets
	 * e.g.
	 * assets/ is replaced with http://wise4.berkeley.edu/curriculum/123/assets/
	 */
	contentString = contentString.replace(/\.\/assets\/|\/assets\/|assets\//gi, fullProjectFolderPath + 'assets/');
	
	return contentString;
};

/**
 * Retrieve path to project folder for current node
 * e.g.
 * "http://wise4.berkeley.edu/curriculum/135/"
 * @return full path to the project folder
 */
View.prototype.getProjectFolderPath = function() {
	var contentBaseUrl = "";
	
	/*
	 * get the content base url which should be the url to the curriculum folder
	 * e.g.
	 * http://wise4.berkeley.edu/curriculum
	 */
	var contentBaseUrl = this.activeNode.getAuthoringModeContentBaseUrl();
	
	//if the contentBaseUrl ends with '/' we will remove it
	if(contentBaseUrl.charAt(contentBaseUrl.length - 1) == '/') {
		contentBaseUrl = contentBaseUrl.substring(0, contentBaseUrl.length - 1);
	}

	var fullProjectFolderPath = null;
	
	if(this.getProjectMetadata().projectFolder != null) {
		/*
		 * the project folder is in the project meta data
		 * e.g.
		 * /135
		 * 
		 * so the full project folder path will look like
		 * http://wise4.berkeley.edu/curriculum/135
		 */
		fullProjectFolderPath = contentBaseUrl + this.getProjectMetadata().projectFolder;
	}
	
	//make sure the projectFolder ends with '/'
	if(fullProjectFolderPath.charAt(fullProjectFolderPath.length - 1) != '/') {
		fullProjectFolderPath += '/';
	}
	
	return fullProjectFolderPath;
};

View.prototype.insertCommonComponents = function() {
	var commonComponents = this[this.resolveType(this.activeNode.type)].getCommonComponents();
	if(commonComponents) {
		for(var x=0; x<commonComponents.length; x++) {
			this['insert' + commonComponents[x]]();
		}		
	}
};

View.prototype.cleanupCommonComponents = function() {
	if (this.activeNode && this.resolveType(this.activeNode.type)) {
		var commonComponents = this[this.resolveType(this.activeNode.type)].getCommonComponents();
		if(commonComponents) {
			for(var x=0; x<commonComponents.length; x++) {
				this['cleanup' + commonComponents[x]]();
			}		
		}
	}
};


/*
 * Prompt functions
 */

View.prototype.insertPrompt = function() {
	this.promptManager.insertPrompt(this);
};

View.prototype.populatePrompt = function() {
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].populatePrompt){
		this[this.resolveType(this.activeNode.type)].populatePrompt();
	}
};

/**
 * Calls the currently authored node's update prompt event if we are in easy
 * mode and one exists, does nothing otherwise.
 */
View.prototype.updatePrompt = function(){
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].updatePrompt){
		this[this.resolveType(this.activeNode.type)].updatePrompt();
	}
};

View.prototype.cleanupPrompt = function() {
	this.promptManager.cleanupPrompt();
};


/*
 * StudentResponseBoxSize functions
 */

View.prototype.insertStudentResponseBoxSize = function() {
	this.studentResponseBoxSizeManager.insertStudentResponseBoxSize(this);
};

View.prototype.populateStudentResponseBoxSize = function() {
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].populateStudentResponseBoxSize){
		this[this.resolveType(this.activeNode.type)].populateStudentResponseBoxSize();
	}
};

View.prototype.updateStudentResponseBoxSize = function() {
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].updateStudentResponseBoxSize){
		this[this.resolveType(this.activeNode.type)].updateStudentResponseBoxSize();
	}
};

View.prototype.cleanupStudentResponseBoxSize = function() {
	this.studentResponseBoxSizeManager.cleanupStudentResponseBoxSize();
};

/*
 * RichTextEditor functions
 */

View.prototype.insertRichTextEditorToggle = function() {
	this.richTextEditorToggleManager.insertRichTextEditorToggle(this);
};

View.prototype.populateRichTextEditorToggle = function() {
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].populateRichTextEditorToggle){
		this[this.resolveType(this.activeNode.type)].populateRichTextEditorToggle();
	}
};

View.prototype.updateRichTextEditorToggle = function() {
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].updateRichTextEditorToggle){
		this[this.resolveType(this.activeNode.type)].updateRichTextEditorToggle();
	}
};

View.prototype.cleanupRichTextEditorToggle = function() {
	this.richTextEditorToggleManager.cleanupRichTextEditorToggle();
};

/**
 * Enables rich text authoring for specified textarea
 * @param id The id of the textarea element on which to activate the rich text editor
 * @param update A callback function to run when the rich text editor content changes
 * @param fullpage A boolean to specify whether to allow full html page editing (default is false)
 */
View.prototype.addRichTextAuthoring = function(id,update,fullpage){
	var target = $('#' + id),
		view = this,
		plugins = "";
	
	//get the context path e.g. /wise
	var contextPath = this.getConfig().getConfigParam('contextPath');
	
	if(fullpage){
		// if full page editing is allowed, include fullpage plugin
		plugins = "fullpage advlist autolink lists link image charmap preview hr anchor \
	        searchreplace wordcount visualblocks visualchars code fullscreen \
	        insertdatetime media table contextmenu directionality spellchecker \
	        template paste textcolor";
	} else {
		plugins = "advlist autolink lists link image charmap preview hr anchor \
	        searchreplace wordcount visualblocks visualchars code fullscreen \
	        insertdatetime media table contextmenu directionality spellchecker \
	        template paste textcolor";
	}
	
	tinymce.init({
	    selector: "#" + id,
	    theme: "modern",
	    skin: "wise",
	    plugins: plugins,
	    allow_script_urls: true,
	    menu : {
	        edit   : {title : 'Edit'  , items : 'undo redo | cut copy paste pastetext | selectall'},
	        insert : {title : 'Insert', items : 'image media link | charmap hr'},
	        view   : {title : 'View'  , items : 'visualaid visualblocks | preview fullscreen'},
	        format : {title : 'Format', items : 'bold italic underline strikethrough superscript subscript | formats | removeformat'},
	        table  : {title : 'Table' , items : 'inserttable tableprops deletetable | cell row column'},
	        tools  : {title : 'Tools' , items : 'code searchreplace'}
	    },
	    toolbar: "undo redo | bold italic | forecolor backcolor | alignleft aligncenter alignright alignjustify | bullist numlist outdent indent | link image media | code",
	    image_advtab: true,
	    templates: [],
	    extended_valid_elements: "a[href|target|title|onclick|name|id|class|style]",
	    document_base_url: view.getProjectFolderPath(),
	    content_css : contextPath + "/vle/css/global.css",
	    relative_urls: false,
	    setup: function(ed){
	    	// add keyUp listener
	        ed.on('keyup change', function(e){
	        	update();
	        });
	    },
	    file_browser_callback : function(field_name, url, type, win) {
	    	fileBrowser(field_name, url, type, win);
	    }
	    
	});
};

function fileBrowser(field_name, url, type, win){
	var callback = function(field_name, url, type, win){
		url = 'assets/' + url;
		win.document.getElementById(field_name).value = url;
		// if we are in an image browser
        if (typeof(win.ImageDialog) != "undefined") {
            // we are, so update image dimensions and preview if necessary
            //if (win.ImageDialog.getImageData) win.ImageDialog.getImageData();
            //if (win.ImageDialog.showPreviewImage) win.ImageDialog.showPreviewImage(url);
        }
        // if we are in a media browser
        if (typeof(win.Media) != "undefined") {
            //if (win.Media.preview) win.Media.preview(); // TODO: fix - preview doesn't seem to work until you switch the media type
            //if (win.MediaDialog.showPreviewImage) win.MediaDialog.showPreviewImage(url);
        }
	};
	var params = {};
	params.field_name = field_name;
	params.type = type;
	params.win = win;
	params.callback = callback;
	eventManager.fire('viewAssets',params);
};

/*
 * StartSentence functions
 */

View.prototype.insertStarterSentenceAuthoring = function() {
	this.starterSentenceAuthoringManager.insertStarterSentenceAuthoring(this);
};

View.prototype.populateStarterSentenceAuthoring = function() {
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].populateStarterSentenceAuthoring){
		this[this.resolveType(this.activeNode.type)].populateStarterSentenceAuthoring();
	}
};

View.prototype.updateStarterSentenceAuthoring = function() {
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].updateStarterSentenceAuthoring){
		this[this.resolveType(this.activeNode.type)].updateStarterSentenceAuthoring();
	}
};

View.prototype.cleanupStarterSentenceAuthoring = function() {
	this.starterSentenceAuthoringManager.cleanupStarterSentenceAuthoring();
};


/*
 * LinkTo functions
 */

View.prototype.cleanupLinkTo = function() {
	this.linkManager.cleanupLinkTo();
};

/*
 * CRater functions
 */

/**
 * Inserts the CRater authoring items
 */
View.prototype.insertCRater = function() {
	this.cRaterManager.insertCRater(this);
};

/**
 * Populates the values in the CRater authoring items with the values
 * from the authored content
 */
View.prototype.populateCRater = function() {
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].populateCRater){
		this[this.resolveType(this.activeNode.type)].populateCRater();
	}
};

/**
 * Updates the CRater authored content
 */
View.prototype.updateCRater = function(){
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].updateCRater){
		this[this.resolveType(this.activeNode.type)].updateCRater();
	}
};

/**
 * The author changed the CRater item id
 */
View.prototype.cRaterItemIdChanged = function(){
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].cRaterItemIdChanged){
		this[this.resolveType(this.activeNode.type)].cRaterItemIdChanged();
	}
};

/**
 * The author changed the CRater type
 */
View.prototype.cRaterItemTypeChangedListener = function(cRaterItemType){
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].cRaterItemTypeChangedListener){
		this[this.resolveType(this.activeNode.type)].cRaterItemTypeChangedListener(cRaterItemType);
	}
};

/**
 * Updates the CRater feedback
 */
View.prototype.updateCRaterFeedback = function(args){
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].updateCRaterFeedback){
		this[this.resolveType(this.activeNode.type)].updateCRaterFeedback(args);
	}
};

/**
 * Updates the CRater display feedback immediately value
 */
View.prototype.updateCRaterDisplayScoreToStudent = function(){
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].updateCRaterDisplayScoreToStudent){
		this[this.resolveType(this.activeNode.type)].updateCRaterDisplayScoreToStudent();
	}
};

/**
 * Updates the CRater display feedback immediately value
 */
View.prototype.updateCRaterDisplayFeedbackToStudent = function(){
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].updateCRaterDisplayFeedbackToStudent){
		this[this.resolveType(this.activeNode.type)].updateCRaterDisplayFeedbackToStudent();
	}
};

/**
 * Updates the CRater must submit and revise before exit value
 */
View.prototype.updateCRaterMustSubmitAndReviseBeforeExit = function(){
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].updateCRaterMustSubmitAndReviseBeforeExit){
		this[this.resolveType(this.activeNode.type)].updateCRaterMustSubmitAndReviseBeforeExit();
	}
};

/**
 * Updates the CRater check work on exit value
 */
View.prototype.updateCRaterCheckWorkOnExit = function(){
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].updateCRaterCheckWorkOnExit){
		this[this.resolveType(this.activeNode.type)].updateCRaterCheckWorkOnExit();
	}
};

/**
 * Updates the CRater must submit and revise before exit value
 */
View.prototype.updateEnableMultipleAttemptFeedbackRules = function(){
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].updateEnableMultipleAttemptFeedbackRules){
		this[this.resolveType(this.activeNode.type)].updateEnableMultipleAttemptFeedbackRules();
	}
};

/**
 * Add a CRater feedback
 */
View.prototype.cRaterAddFeedback = function(args){
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].cRaterAddFeedback){
		this[this.resolveType(this.activeNode.type)].cRaterAddFeedback(args);
	}
};

/**
 * Remove a CRater feedback
 */
View.prototype.cRaterRemoveFeedback = function(args){
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].cRaterRemoveFeedback){
		this[this.resolveType(this.activeNode.type)].cRaterRemoveFeedback(args);
	}
};

/**
 * Update the CRater max check answers value
 */
View.prototype.updateCRaterMaxCheckAnswers = function(){
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].updateCRaterMaxCheckAnswers){
		this[this.resolveType(this.activeNode.type)].updateCRaterMaxCheckAnswers();
	}
};

/**
 * Removes the CRater authoring items from the authorstep page and
 * clears input values
 */
View.prototype.cleanupCRater = function() {
	this.cRaterManager.cleanupCRater();
};

/**
 * Enable CRater for this step
 */
View.prototype.updateEnableCRater = function() {
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].updateEnableCRater){
		this[this.resolveType(this.activeNode.type)].updateEnableCRater();
	}
};

/**
 * Inserts the Step Icons authoring items
 */
View.prototype.insertStepIcons = function() {
	this.stepIconsManager.insertStepIcons(this);
};

/**
 * Inserts the Step Icons authoring items
 */
View.prototype.cleanupStepIcons = function() {
	this.stepIconsManager.cleanupStepIcons(this);
};

/**
 * Update the CRater student action value
 */
View.prototype.cRaterStudentActionUpdated = function(args){
	if(this.easyMode && this[this.resolveType(this.activeNode.type)] && this[this.resolveType(this.activeNode.type)].cRaterStudentActionUpdated){
		this[this.resolveType(this.activeNode.type)].cRaterStudentActionUpdated(args);
	}
};

//used to notify scriptloader that this script has finished loading
if(typeof eventManager != 'undefined'){
	eventManager.fire('scriptLoaded', 'vle/view/authoring/authorview_authorstep.js');
}