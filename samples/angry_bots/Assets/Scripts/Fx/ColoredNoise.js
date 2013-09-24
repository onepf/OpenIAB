
#pragma strict

@script ExecuteInEditMode

@script RequireComponent (Camera)
@script AddComponentMenu ("Image Effects/Noise") 

public var globalNoiseAmount : float = 0.075f;
public var globalNoiseAmountOnDamage : float = -6.0f;

public var localNoiseAmount : float = 0.0f;
private var noiseShader : Shader;
public var noiseTexture : Texture2D;
private var noise : Material = null;

function Start () {
	FindShaders ();
	CheckSupport ();
	CreateMaterials ();	
}

function FindShaders () {	
	if (!noiseShader)
		noiseShader = Shader.Find("Hidden/ColoredNoise");
}

function CreateMaterials () {		
	if (!noise) {
		noise = new Material (noiseShader);	
		noise.hideFlags = HideFlags.DontSave;
	}           
}

function Supported () : boolean {
	return (SystemInfo.supportsImageEffects && SystemInfo.supportsRenderTextures && noiseShader.isSupported);
}

function CheckSupport () : boolean {
	if (!Supported ()) {
		enabled = false;
		return false;
	}	
	return true;
}

function OnDisable () {
	if (noise) {
		DestroyImmediate (noise);
		noise = null;	
	}
}

function OnRenderImage (source : RenderTexture, destination : RenderTexture) {		
#if UNITY_EDITOR
	FindShaders ();
	CheckSupport ();
	CreateMaterials ();	
#endif
						
	noise.SetFloat ("_NoiseAmount", globalNoiseAmount + localNoiseAmount * Mathf.Sign (globalNoiseAmount));
	noise.SetTexture ("_NoiseTex", noiseTexture);	

	DrawNoiseQuadGrid (source, destination, noise, noiseTexture, 0);
}

// Helper to draw a screenspace grid of quads with random texture coordinates

static function DrawNoiseQuadGrid (source : RenderTexture, dest : RenderTexture, fxMaterial : Material, noise : Texture2D, passNr : int) {
	RenderTexture.active = dest;
	
	#if UNITY_XBOX360
	    var tileSize : float = 128.0f;
	#else
	    var tileSize : float = 64.0f;
	#endif
	
	var subDs : float = (1.0f * source.width) / tileSize;
       
	fxMaterial.SetTexture ("_MainTex", source);	        
                
	GL.PushMatrix ();
	GL.LoadOrtho ();	
		
	var aspectCorrection : float = (1.0f * source.width) / (1.0f * source.height);
	var stepSizeX : float = 1.0f / subDs;
	var stepSizeY : float = stepSizeX * aspectCorrection; 
   	var texTile : float = tileSize / (noise.width * 1.0f);
    	    	
	fxMaterial.SetPass (passNr);	
	
    GL.Begin (GL.QUADS);
    
   	for (var x1 : float = 0.0; x1 < 1.0; x1 += stepSizeX) {
   		for (var y1 : float = 0.0; y1 < 1.0; y1 += stepSizeY) { 
   			
   			var tcXStart : float = Random.Range (-1.0f, 1.0f);
   			var tcYStart : float = Random.Range (-1.0f, 1.0f);
   			var texTileMod : float = Mathf.Sign (Random.Range (-1.0f, 1.0f));
						
		    GL.MultiTexCoord2 (0, tcXStart, tcYStart); 
		    GL.Vertex3 (x1, y1, 0.1);
		    GL.MultiTexCoord2 (0, tcXStart + texTile * texTileMod, tcYStart); 
		    GL.Vertex3 (x1 + stepSizeX, y1, 0.1);
		    GL.MultiTexCoord2 (0, tcXStart + texTile * texTileMod, tcYStart + texTile * texTileMod); 
		    GL.Vertex3 (x1 + stepSizeX, y1 + stepSizeY, 0.1);
		    GL.MultiTexCoord2 (0, tcXStart, tcYStart + texTile * texTileMod); 
		    GL.Vertex3 (x1, y1 + stepSizeY, 0.1);
   		}
   	}
    	
	GL.End ();
    GL.PopMatrix ();
}
