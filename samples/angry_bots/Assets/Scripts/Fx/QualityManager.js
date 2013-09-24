

// QualityManager sets shader LOD's and enabled/disables special effects
// based on platform and/or desired quality settings.

// Disable 'autoChoseQualityOnStart' if you want to overwrite the quality
// for a specific platform with the desired level.

#pragma strict

@script ExecuteInEditMode
@script RequireComponent (Camera)
@script RequireComponent (ShaderDatabase)

// Quality enum values will be used directly for shader LOD settings

enum Quality {
	Lowest = 100,
	Poor = 190,
	Low = 200,
	Medium = 210,
	High = 300,
	Highest = 500,	
}

public var autoChoseQualityOnStart : boolean = true;
public var currentQuality : Quality = Quality.Highest;

public var bloom : MobileBloom;
public var depthOfField : HeightDepthOfField;
public var noise : ColoredNoise;
public var heightFog : RenderFogPlane;
public var reflection : MonoBehaviour;
public var shaders : ShaderDatabase;

public static var quality : Quality = Quality.Highest;

function Awake () {
	if (!bloom)
		bloom = GetComponent.<MobileBloom> ();
	if (!noise)
		noise = GetComponent.<ColoredNoise> ();
	if (!depthOfField)
		depthOfField = GetComponent.<HeightDepthOfField> ();
	if (!heightFog)
		heightFog = gameObject.GetComponentInChildren.<RenderFogPlane> ();
	if (!shaders)
		shaders = GetComponent.<ShaderDatabase> ();
	if (!reflection)
		reflection = GetComponent ("ReflectionFx") as MonoBehaviour;

	if (autoChoseQualityOnStart) 
		AutoDetectQuality ();	

	ApplyAndSetQuality (currentQuality);
}

// we support dynamic quality adjustments if in edit mode

#if UNITY_EDITOR

function Update () {
	var newQuality : Quality = currentQuality; 
	if (newQuality != quality) 
		ApplyAndSetQuality (newQuality);	
}

#endif

private function AutoDetectQuality () {
		
#if UNITY_IPHONE	

	// some special quality settings cases for various platforms

	if (iPhoneSettings.generation == iPhoneGeneration.iPad1Gen) {
		currentQuality = Quality.Low;
		Debug.Log("AngryBots: quality set to 'Low' (iPad1 class iOS)");		
	}
	else if (iPhoneSettings.generation == iPhoneGeneration.iPad2Gen) {
		currentQuality = Quality.High;
		Debug.Log("AngryBots: quality set to 'High' (iPad2 class iOS)");		
	}
	else if (iPhoneSettings.generation == iPhoneGeneration.iPhone3GS || iPhoneSettings.generation == iPhoneGeneration.iPodTouch3Gen) {
		currentQuality = Quality.Low;
		Debug.Log("AngryBots: quality set to 'Low' (iPhone 3GS class iOS)");					
	}
	else {
		currentQuality = Quality.Medium;
		Debug.Log("AngryBots: quality set to 'Medium' (iPhone4 class iOS)");		
	}
		
#else

#if UNITY_ANDROID

	Debug.Log("AngryBots: quality set to 'Low' (current default for all Android)");	
	currentQuality = Quality.Low;
		
#else

	// quality for desktops/consoles

	if (SystemInfo.graphicsPixelFillrate < 2800) {
		currentQuality = Quality.High;	
		Debug.Log("AngryBots: quality set to 'High'");		
	}
	else {
		currentQuality = Quality.Highest;	
		Debug.Log("AngryBots: quality set to 'Highest'");
	}
		
#endif	

#endif
}

private function ApplyAndSetQuality (newQuality : Quality) {	
	quality = newQuality;

	// default states
	
	camera.cullingMask = -1 && ~(1 << LayerMask.NameToLayer ("Adventure"));
	var textAdventure : GameObject = GameObject.Find ("TextAdventure");		
	if (textAdventure) 
		textAdventure.GetComponent.<TextAdventureManager> ().enabled = false;
			
	// check for quality specific states		
			
	if (quality == Quality.Lowest) {
		DisableAllFx ();	
		if (textAdventure) 
			textAdventure.GetComponent.<TextAdventureManager> ().enabled = true;
		camera.cullingMask = 1 << LayerMask.NameToLayer ("Adventure");
		EnableFx (depthOfField, false);	
		EnableFx (heightFog, false);				
		EnableFx (bloom, false);	
		EnableFx (noise, false);									
		camera.depthTextureMode = DepthTextureMode.None;
	}
	else if (quality == Quality.Poor) {
		EnableFx (depthOfField, false);	
		EnableFx (heightFog, false);				
		EnableFx (bloom, false);		
		EnableFx (noise, false);				
		EnableFx (reflection, false);	
		camera.depthTextureMode = DepthTextureMode.None;						
	} 
	else if (quality == Quality.Low) {
		EnableFx (depthOfField, false);	
		EnableFx (heightFog, false);				
		EnableFx (bloom, false);		
		EnableFx (noise, false);				
		EnableFx (reflection, true);	
		camera.depthTextureMode = DepthTextureMode.None;						
	} 
	else if (quality == Quality.Medium) {
		EnableFx (depthOfField, false);	
		EnableFx (heightFog, false);				
		EnableFx (bloom, true);		
		EnableFx (noise, false);						
		EnableFx (reflection, true);		
		camera.depthTextureMode = DepthTextureMode.None;										
	} 
	else if (quality == Quality.High) {
		EnableFx (depthOfField, false);	
		EnableFx (heightFog, false);				
		EnableFx (bloom, true);		
		EnableFx (noise, true);				
		EnableFx (reflection, true);
		camera.depthTextureMode = DepthTextureMode.None;							
	} 
	else { // Highest
		EnableFx (depthOfField, true);	
		EnableFx (heightFog, true);				
		EnableFx (bloom, true);		
		EnableFx (reflection, true);
		EnableFx (noise, true);					
		if ((heightFog && heightFog.enabled) || (depthOfField && depthOfField.enabled))
			camera.depthTextureMode |= DepthTextureMode.Depth;	
	}
	
	Debug.Log ("AngryBots: setting shader LOD to " + quality);
	
	Shader.globalMaximumLOD = quality;
	for (var s : Shader in shaders.shaders) {
		s.maximumLOD = quality;	
	}
}

private function DisableAllFx () {
	camera.depthTextureMode = DepthTextureMode.None;
	EnableFx (reflection, false);	
	EnableFx (depthOfField, false);	
	EnableFx (heightFog, false);				
	EnableFx (bloom, false);	
	EnableFx (noise, false);					
}

private function EnableFx (fx : MonoBehaviour, enable : boolean) {
	if (fx)
		fx.enabled = enable;
}