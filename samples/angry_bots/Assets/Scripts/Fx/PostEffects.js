
#pragma strict

class PostEffects {
			
	static function CheckShaderAndCreateMaterial(s : Shader, m2Create : Material) : Material {
		if (m2Create && m2Create.shader == s) 
			return m2Create;
			
		if (!s) { 
			Debug.LogWarning("PostEffects: missing shader for " + m2Create.ToString ());
			return null;
		}
		
		if(!s.isSupported) {
			Debug.LogWarning ("The shader " + s.ToString () + " is not supported");
			return null;
		}
		else {
			m2Create = new Material (s);	
			m2Create.hideFlags = HideFlags.DontSave;		
			return m2Create;
		}
	}
		
	static function CheckSupport (needDepth : boolean) : boolean {		
		if (!SystemInfo.supportsImageEffects || !SystemInfo.supportsRenderTextures) {
			Debug.Log ("Disabling image effect as this platform doesn't support any");
			return false;
		}	
		
		if(needDepth && !SystemInfo.SupportsRenderTextureFormat (RenderTextureFormat.Depth)) {
			Debug.Log ("Disabling image effect as depth textures are not supported on this platform.");
			return false;
		}
		
		return true;
	}
}