/*
 * the scripts that are always necessary regardless of whether the
 * user is using the vle, authoring tool, or grading tool
 */
var coreScripts = [
	'vle/node/surge/SurgeNode.js',
	'vle/node/surge/surgeEvents.js'
];

//the scripts used in the vle
var studentVLEScripts = [
	scriptloader.jquerySrc,
	scriptloader.jqueryUISrc,
	'vle/node/common/nodehelpers.js',
	'vle/node/surge/surge.js',
	'vle/node/surge/surgeState.js'
];

//the scripts used in the authoring tool
var authorScripts = [
	'vle/node/surge/authorview_surge.js'
];

//the scripts used in the grading tool
var gradingScripts = [
	'vle/node/surge/surgeState.js'
];

//dependencies when a file requires another file to be loaded before it
var dependencies = [
	{child:"vle/node/surge/SurgeNode.js", parent:["vle/node/Node.js"]}
];

var nodeClasses = [
	{nodeClass:'display', nodeClassText:'Surge', icon:'node/surge/icons/display28.png'}
];

var nodeIconPath = 'node/surge/icons/';
componentloader.addNodeIconPath('SurgeNode', nodeIconPath);

scriptloader.addScriptToComponent('core', coreScripts);
scriptloader.addScriptToComponent('surge', studentVLEScripts);
scriptloader.addScriptToComponent('author', authorScripts);
scriptloader.addScriptToComponent('studentwork', gradingScripts);
scriptloader.addDependencies(dependencies);

componentloader.addNodeClasses('SurgeNode', nodeClasses);

var css = [
       	"vle/node/surge/surge.css"
];

scriptloader.addCssToComponent('surge', css);

var nodeTemplateParams = [
	{
		nodeTemplateFilePath:'node/surge/surgeTemplate.su',
		nodeExtension:'su'
	}
];

componentloader.addNodeTemplateParams('SurgeNode', nodeTemplateParams);

//used to notify scriptloader that this script has finished loading
if(typeof eventManager != 'undefined'){
	eventManager.fire('scriptLoaded', 'vle/node/surge/setup.js');
};