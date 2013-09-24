
#pragma strict

@script ExecuteInEditMode

@script RequireComponent (Camera)
@script AddComponentMenu ("Image Effects/Height Depth of Field") 

enum DofQualitySetting {
	OnlyBackground = 1,
	BackgroundAndForeground = 2,
}

enum DofResolution{
	High = 2,
	Medium = 3,
	Low = 4,	
}
	
public var resolution : DofResolution  = DofResolution.High;
	
public var objectFocus : Transform = null;
 
public var maxBlurSpread : float = 1.55;
public var foregroundBlurExtrude : float = 1.055;
public var smoothness : float = 1.0f;
		
private var dofBlurShader : Shader;
private var dofBlurMaterial : Material = null;	

private var dofShader : Shader;
private var dofMaterial : Material = null;
   
public var visualize : boolean = false;
   
private var widthOverHeight : float = 1.25f;
private var oneOverBaseSize : float = 1.0f / 512.0f;

private var cameraNear : float = 0.5f;
private var cameraFar : float = 50.0f;
private var cameraFov : float = 60.0f;	
private var cameraAspect : float = 1.333333f;

function Start () {
	FindShaders ();
	CheckSupport ();
	CreateMaterials ();	
}

function FindShaders () {	
	if (!dofBlurShader)
		dofBlurShader = Shader.Find("Hidden/BlurPassesForDOF");
	if (!dofShader)
		dofShader = Shader.Find("Hidden/HeightDepthOfField");	
}

function CreateMaterials () {		
	if (!dofBlurMaterial)
		dofBlurMaterial = PostEffects.CheckShaderAndCreateMaterial (dofBlurShader, dofBlurMaterial);
	if (!dofMaterial)
		dofMaterial = PostEffects.CheckShaderAndCreateMaterial (dofShader, dofMaterial);           
}

function Supported () : boolean {
	return (PostEffects.CheckSupport (true) && dofBlurShader.isSupported && dofShader.isSupported);
}

function CheckSupport () : boolean {
	if (!Supported ()) {
		enabled = false;
		return false;
	}	
	return true;
}

function OnDisable () {
	if (dofBlurMaterial) {
		DestroyImmediate (dofBlurMaterial);
		dofBlurMaterial = null;	
	}	
	if (dofMaterial) {
		DestroyImmediate (dofMaterial);
		dofMaterial = null;	
	}
}

function OnRenderImage (source : RenderTexture, destination : RenderTexture) {	
#if UNITY_EDITOR
	FindShaders ();
	CheckSupport ();
	CreateMaterials ();	
#endif
	
	widthOverHeight = (1.0f * source.width) / (1.0f * source.height);
	oneOverBaseSize = 1.0f / 512.0f;		
	
	cameraNear = camera.nearClipPlane;
	cameraFar = camera.farClipPlane;
	cameraFov = camera.fieldOfView;
	cameraAspect = camera.aspect;

	var frustumCorners : Matrix4x4 = Matrix4x4.identity;		
	var vec : Vector4;
	var corner : Vector3;
	var fovWHalf : float = cameraFov * 0.5f;
	var toRight : Vector3 = camera.transform.right * cameraNear * Mathf.Tan (fovWHalf * Mathf.Deg2Rad) * cameraAspect;
	var toTop : Vector3 = camera.transform.up * cameraNear * Mathf.Tan (fovWHalf * Mathf.Deg2Rad);
	var topLeft : Vector3 = (camera.transform.forward * cameraNear - toRight + toTop);
	var cameraScaleFactor : float = topLeft.magnitude * cameraFar/cameraNear;	
		
	topLeft.Normalize();
	topLeft *= cameraScaleFactor;

	var topRight : Vector3 = (camera.transform.forward * cameraNear + toRight + toTop);
	topRight.Normalize();
	topRight *= cameraScaleFactor;
	
	var bottomRight : Vector3 = (camera.transform.forward * cameraNear + toRight - toTop);
	bottomRight.Normalize();
	bottomRight *= cameraScaleFactor;
	
	var bottomLeft : Vector3 = (camera.transform.forward * cameraNear - toRight - toTop);
	bottomLeft.Normalize();
	bottomLeft *= cameraScaleFactor;
			
	frustumCorners.SetRow (0, topLeft); 
	frustumCorners.SetRow (1, topRight);		
	frustumCorners.SetRow (2, bottomRight);
	frustumCorners.SetRow (3, bottomLeft);	
	
	dofMaterial.SetMatrix ("_FrustumCornersWS", frustumCorners);
	dofMaterial.SetVector ("_CameraWS", camera.transform.position);			
	
	var t : Transform;
	if (!objectFocus)
		t = camera.transform;
	else
		t = objectFocus.transform;
																	
	dofMaterial.SetVector ("_ObjectFocusParameter", Vector4 (	
				t.position.y - 0.25f, t.localScale.y * 1.0f / smoothness, 1.0f, objectFocus ? objectFocus.collider.bounds.extents.y * 0.75f : 0.55f));
       		
	dofMaterial.SetFloat ("_ForegroundBlurExtrude", foregroundBlurExtrude);
	dofMaterial.SetVector ("_InvRenderTargetSize", Vector4 (1.0 / (1.0 * source.width), 1.0 / (1.0 * source.height),0.0,0.0));
	
	var divider : int = 1;
	if (resolution == DofResolution.Medium)
		divider = 2;
	else if (resolution >= DofResolution.Medium)
		divider = 3;
	
	var hrTex : RenderTexture = RenderTexture.GetTemporary (source.width, source.height, 0); 
	var mediumTexture : RenderTexture = RenderTexture.GetTemporary (source.width / divider, source.height / divider, 0);    
	var mediumTexture2 : RenderTexture = RenderTexture.GetTemporary (source.width / divider, source.height / divider, 0);    
	var lowTexture : RenderTexture = RenderTexture.GetTemporary (source.width / (divider * 2), source.height / (divider * 2), 0);     
	
	source.filterMode = FilterMode.Bilinear;
	hrTex.filterMode = FilterMode.Bilinear;   
	lowTexture.filterMode = FilterMode.Bilinear;     
	mediumTexture.filterMode = FilterMode.Bilinear;
	mediumTexture2.filterMode = FilterMode.Bilinear;
	
    // background (coc -> alpha channel)
   	CustomGraphicsBlit (null, source, dofMaterial, 3);		
   		
   	// better downsample (should actually be weighted for higher quality)
   	mediumTexture2.DiscardContents();
   	Graphics.Blit (source, mediumTexture2, dofMaterial, 6);	
			
	Blur (mediumTexture2, mediumTexture, 1, 0, maxBlurSpread * 0.75f);			
	Blur (mediumTexture, lowTexture, 2, 0, maxBlurSpread);			
    	      		
	// some final calculations can be performed in low resolution 		
	dofBlurMaterial.SetTexture ("_TapLow", lowTexture);
	dofBlurMaterial.SetTexture ("_TapMedium", mediumTexture);							
	Graphics.Blit (null, mediumTexture2, dofBlurMaterial, 2);
	
	dofMaterial.SetTexture ("_TapLowBackground", mediumTexture2); 
	dofMaterial.SetTexture ("_TapMedium", mediumTexture); // only needed for debugging		
							
	// apply background defocus
	hrTex.DiscardContents();
	Graphics.Blit (source, hrTex, dofMaterial, visualize ? 2 : 0); 
	
	// foreground handling
	CustomGraphicsBlit (hrTex, source, dofMaterial, 5); 
	
	// better downsample and blur (shouldn't be weighted)
	Graphics.Blit (source, mediumTexture2, dofMaterial, 6);					
	Blur (mediumTexture2, mediumTexture, 1, 1, maxBlurSpread * 0.75f);	
	Blur (mediumTexture, lowTexture, 2, 1, maxBlurSpread);	
	
	// some final calculations can be performed in low resolution		
	dofBlurMaterial.SetTexture ("_TapLow", lowTexture);
	dofBlurMaterial.SetTexture ("_TapMedium", mediumTexture);							
	Graphics.Blit (null, mediumTexture2, dofBlurMaterial, 2);	
	
	if (destination != null)
	    destination.DiscardContents ();
	    
	dofMaterial.SetTexture ("_TapLowForeground", mediumTexture2); 
	dofMaterial.SetTexture ("_TapMedium", mediumTexture); // only needed for debugging	   
	Graphics.Blit (source, destination, dofMaterial, visualize ? 1 : 4);	
	
	RenderTexture.ReleaseTemporary (hrTex);
	RenderTexture.ReleaseTemporary (mediumTexture);
	RenderTexture.ReleaseTemporary (mediumTexture2);
	RenderTexture.ReleaseTemporary (lowTexture);
}	

// flat blur

function Blur (from : RenderTexture, to : RenderTexture, iterations : int, blurPass: int, spread : float) {
	var tmp : RenderTexture = RenderTexture.GetTemporary (to.width, to.height, 0);
	
	if (iterations < 2) {
		dofBlurMaterial.SetVector ("offsets", Vector4 (0.0, spread * oneOverBaseSize, 0.0, 0.0));
		tmp.DiscardContents ();
		Graphics.Blit (from, tmp, dofBlurMaterial, blurPass);
	
		dofBlurMaterial.SetVector ("offsets", Vector4 (spread / widthOverHeight * oneOverBaseSize,  0.0, 0.0, 0.0));		
		to.DiscardContents ();
		Graphics.Blit (tmp, to, dofBlurMaterial, blurPass);	 	
	} 
	else {	
		dofBlurMaterial.SetVector ("offsets", Vector4 (0.0, spread * oneOverBaseSize, 0.0, 0.0));
		tmp.DiscardContents ();
		Graphics.Blit (from, tmp, dofBlurMaterial, blurPass);
		
		dofBlurMaterial.SetVector ("offsets", Vector4 (spread / widthOverHeight * oneOverBaseSize,  0.0, 0.0, 0.0));		
		to.DiscardContents ();
		Graphics.Blit (tmp, to, dofBlurMaterial, blurPass);	 
	
		dofBlurMaterial.SetVector ("offsets", Vector4 (spread / widthOverHeight * oneOverBaseSize,  spread * oneOverBaseSize, 0.0, 0.0));		
		tmp.DiscardContents ();
		Graphics.Blit (to, tmp, dofBlurMaterial, blurPass);	
	
		dofBlurMaterial.SetVector ("offsets", Vector4 (spread / widthOverHeight * oneOverBaseSize,  -spread * oneOverBaseSize, 0.0, 0.0));		
		to.DiscardContents ();
		Graphics.Blit (tmp, to, dofBlurMaterial, blurPass);	
	}
	
	RenderTexture.ReleaseTemporary (tmp);
}

// used for noise

function CustomGraphicsBlit (source : RenderTexture, dest : RenderTexture, fxMaterial : Material, passNr : int) {
	RenderTexture.active = dest;
	       
	fxMaterial.SetTexture ("_MainTex", source);	        
        	        
	GL.PushMatrix ();
	GL.LoadOrtho ();	
    	
	fxMaterial.SetPass (passNr);	
	
    GL.Begin (GL.QUADS);
						
	GL.MultiTexCoord2 (0, 0.0f, 0.0f); 
	GL.Vertex3 (0.0f, 0.0f, 3.0f); // BL
	
	GL.MultiTexCoord2 (0, 1.0f, 0.0f); 
	GL.Vertex3 (1.0f, 0.0f, 2.0f); // BR
	
	GL.MultiTexCoord2 (0, 1.0f, 1.0f); 
	GL.Vertex3 (1.0f, 1.0f, 1.0f); // TR
	
	GL.MultiTexCoord2 (0, 0.0f, 1.0f); 
	GL.Vertex3 (0.0f, 1.0f, 0.0); // TL
	
	GL.End ();
    GL.PopMatrix ();
}	
