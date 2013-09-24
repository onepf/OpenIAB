
#pragma strict

@script ExecuteInEditMode

@script RequireComponent (Camera)
@script AddComponentMenu ("Image Effects/Mobile Bloom") 

public var intensity : float = 0.5f;
public var colorMix : Color = Color.white;
public var colorMixBlend : float = 0.25f;
public var agonyTint : float = 0.0f;

private var bloomShader : Shader;
private var apply : Material = null;
private var rtFormat : RenderTextureFormat = RenderTextureFormat.Default;

function Start () {
	FindShaders ();
	CheckSupport ();
	CreateMaterials ();	
}

function FindShaders () {	
	if (!bloomShader)
		bloomShader = Shader.Find("Hidden/MobileBloom");
}

function CreateMaterials () {		
	if (!apply) {
		apply = new Material (bloomShader);	
		apply.hideFlags = HideFlags.DontSave;
	}           
}

function OnDamage () {
	agonyTint = 1.0f;	
}

function Supported () : boolean {
	return (SystemInfo.supportsImageEffects && SystemInfo.supportsRenderTextures && bloomShader.isSupported);
}

function CheckSupport () : boolean {
	if (!Supported ()) {
		enabled = false;
		return false;
	}	
	rtFormat = SystemInfo.SupportsRenderTextureFormat (RenderTextureFormat.RGB565) ? RenderTextureFormat.RGB565 : RenderTextureFormat.Default;
	return true;
}

function OnDisable () {
	if (apply) {
		DestroyImmediate (apply);
		apply = null;	
	}
}

function OnRenderImage (source : RenderTexture, destination : RenderTexture) {		
#if UNITY_EDITOR
	FindShaders ();
	CheckSupport ();
	CreateMaterials ();	
#endif

	agonyTint = Mathf.Clamp01 (agonyTint - Time.deltaTime * 2.75f);
		
	var tempRtLowA : RenderTexture = RenderTexture.GetTemporary (source.width / 4, source.height / 4, rtFormat);
	var tempRtLowB : RenderTexture = RenderTexture.GetTemporary (source.width / 4, source.height / 4, rtFormat);
	
	// prepare data
	
	apply.SetColor ("_ColorMix", colorMix);
	apply.SetVector ("_Parameter", Vector4 (colorMixBlend * 0.25f,  0.0f, 0.0f, 1.0f - intensity - agonyTint));	
	
	// downsample & blur
	
	Graphics.Blit (source, tempRtLowA, apply, agonyTint < 0.5f ? 1 : 5);
	Graphics.Blit (tempRtLowA, tempRtLowB, apply, 2);
	Graphics.Blit (tempRtLowB, tempRtLowA, apply, 3);
	
	// apply
	
	apply.SetTexture ("_Bloom", tempRtLowA);
	Graphics.Blit (source, destination, apply, QualityManager.quality > Quality.Medium ? 4 : 0);
	
	RenderTexture.ReleaseTemporary (tempRtLowA);
	RenderTexture.ReleaseTemporary (tempRtLowB);
}
