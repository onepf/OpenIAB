
#pragma strict

@script ExecuteInEditMode ()

public var casterA : Transform; 
public var colorA : Color = Color.white;
public var casterB : Transform; 
public var colorB : Color = Color.white;
public var casterC : Transform; 
public var colorC : Color = Color.white;

function Update () {
	if (casterA)
		Shader.SetGlobalVector ("SPEC_LIGHT_DIR_0", casterA.forward);
	if (casterB)
		Shader.SetGlobalVector ("SPEC_LIGHT_DIR_1", casterB.forward);
	if (casterC)
		Shader.SetGlobalVector ("SPEC_LIGHT_DIR_2", casterC.forward);
	
	Shader.SetGlobalVector ("SPEC_LIGHT_COLOR_0", colorA);
	Shader.SetGlobalVector ("SPEC_LIGHT_COLOR_1", colorB);
	Shader.SetGlobalVector ("SPEC_LIGHT_COLOR_2", colorC);
}