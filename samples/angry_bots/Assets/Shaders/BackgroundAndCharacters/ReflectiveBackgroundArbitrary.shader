
/* 

one of the most common shader in AngryBots

same as ReflectiveBackgroundPlanarGeometry but falls back
to a simpler lightmap-apply-only shader.

as this shader is used for arbitrary geometry, it is biasing the
normal a little towards the viewer to create a less dramatic specular
look

*/

Shader "AngryBots/ReflectiveBackgroundArbitraryGeometry" {
	
Properties {
	_MainTex ("Base", 2D) = "white" {}
	_Normal("Normal", 2D) = "bump" {}
	_Cube("Cube", CUBE) = "black" {}
	_OneMinusReflectivity("OneMinusReflectivity", Range(0.0, 1.0)) = 0.05
}


CGINCLUDE		

struct v2f 
{
	half4 pos : SV_POSITION;
	half4 uv : TEXCOORD0;
};

struct v2f_full
{
	half4 pos : SV_POSITION;
	half4 uv : TEXCOORD0;
	half3 tsBase0 : TEXCOORD2;
	half3 tsBase1 : TEXCOORD3;
	half3 tsBase2 : TEXCOORD4;	
	half3 viewDir : TEXCOORD5;
};
	
#include "UnityCG.cginc"		

sampler2D _MainTex;
sampler2D _Normal;		
samplerCUBE _Cube;

half _OneMinusReflectivity;
						
ENDCG 

SubShader {
	Tags { "RenderType"="Opaque" }
	LOD 300 
	
	Pass {
		CGPROGRAM
		
		#include "AngryInclude.cginc"
		
		float4 unity_LightmapST;
		sampler2D unity_Lightmap;	
		float4 _MainTex_ST;
				
		v2f_full vert (appdata_full v) 
		{
			v2f_full o;
			
			o.pos = mul (UNITY_MATRIX_MVP, v.vertex);
			o.uv.xy = TRANSFORM_TEX(v.texcoord,_MainTex);
			
			#ifdef LIGHTMAP_ON
				o.uv.zw = v.texcoord1.xy * unity_LightmapST.xy + unity_LightmapST.zw;
			#else
				o.uv.zw = half2(0,0);
			#endif
			
			o.viewDir = normalize(WorldSpaceViewDir(v.vertex));
			
			WriteTangentSpaceData(v, o.tsBase0,o.tsBase1,o.tsBase2);
				
			return o; 
		}
				
		
		fixed4 frag (v2f_full i) : COLOR0 
		{
			half3 nrml = UnpackNormal(tex2D(_Normal, i.uv.xy));
			half3 bumpedNormal = (half3(dot(i.tsBase0,nrml), dot(i.tsBase1,nrml), dot(i.tsBase2,nrml)));
			
			bumpedNormal = (bumpedNormal + i.viewDir.xyz) * 0.5;			
			
			fixed4 tex = tex2D (_MainTex, i.uv.xy);			
			half3 reflectVector = reflect(-i.viewDir.xyz, bumpedNormal.xyz);
			
			fixed4 reflection = texCUBE (_Cube, reflectVector);
			
			tex += reflection * saturate(tex.a - _OneMinusReflectivity);
												
			#ifdef LIGHTMAP_ON
				fixed3 lm =  DecodeLightmap (tex2D(unity_Lightmap, i.uv.zw));
				tex.rgb *= lm;
			#else
				tex.rgb *= 0.75;
			#endif	
						
			return tex;
			
		}		
		
		#pragma vertex vert
		#pragma fragment frag
		#pragma fragmentoption ARB_precision_hint_fastest 
		#pragma multi_compile LIGHTMAP_OFF LIGHTMAP_ON
	
		ENDCG
	}
} 

FallBack "AngryBots/Fallback"
}

