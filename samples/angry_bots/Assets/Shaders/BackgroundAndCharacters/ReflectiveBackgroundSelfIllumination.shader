/* 

same as ReflectiveBackgroundArbitraryGeometry but .a channel defines
additional self illumination while the reflection mask is a simple heuristic
based on the texture luminance or a single color channel.

*/

Shader "Self-Illumin/AngryBots/ReflectiveBackgroundSelfIllumination" {
	
Properties {
	_MainTex ("Base", 2D) = "white" {}
	_Cube ("Cube", Cube) = "" {}
	_Normal("Normal", 2D) = "bump" {}
	_EmissionLM ("Emission (Lightmapper)", Float) = 0	
	_OneMinusReflectivity("OneMinusReflectivity", Range(0.0, 1.0)) = 0.1
}

CGINCLUDE		


struct v2f 
{
	half4 pos : SV_POSITION;
	half2 uv : TEXCOORD0;
	#ifdef LIGHTMAP_ON
		half2 uvLM : TEXCOORD1;
	#endif	
};

struct v2f_full
{
	half4 pos : SV_POSITION;
	half2 uv : TEXCOORD0;
	half3 worldViewDir : TEXCOORD1;
	half3 tsBase0 : TEXCOORD2;
	half3 tsBase1 : TEXCOORD3;
	half3 tsBase2 : TEXCOORD4;	
	#ifdef LIGHTMAP_ON
		half2 uvLM : TEXCOORD5;
	#endif	
};
	
#include "AngryInclude.cginc"		

sampler2D _MainTex;
samplerCUBE _Cube;
sampler2D _Normal;		

half _OneMinusReflectivity;
						
ENDCG 

// tangent space forward shader with simple cube lookup & normal map

SubShader {
	Tags { "RenderType"="Opaque" "Reflection" = "RenderReflectionOpaque"}
	LOD 300 
	
	Pass {
		CGPROGRAM
		
		float4 unity_LightmapST;
		sampler2D unity_Lightmap;	
		float4 _MainTex_ST;
				
		v2f_full vert (appdata_full v) 
		{
			v2f_full o;
			o.pos = mul (UNITY_MATRIX_MVP, v.vertex);
			o.uv = TRANSFORM_TEX(v.texcoord,_MainTex);
			
			#ifdef LIGHTMAP_ON
				o.uvLM = v.texcoord1.xy * unity_LightmapST.xy + unity_LightmapST.zw;
			#endif
			
			o.worldViewDir = normalize(WorldSpaceViewDir(v.vertex));
						
			WriteTangentSpaceData(v, o.tsBase0, o.tsBase1, o.tsBase2);	
				
			return o; 
		}
				
		
		fixed4 frag (v2f_full i) : COLOR0 
		{
			half3 nrml = UnpackNormal(tex2D(_Normal, i.uv.xy));
			half3 bumpedNormal = half3(dot(i.tsBase0,nrml), dot(i.tsBase1,nrml), dot(i.tsBase2,nrml));

			fixed4 tex = tex2D (_MainTex, i.uv.xy);
			
			bumpedNormal = (bumpedNormal + i.worldViewDir.xyz) * 0.5;
			
			half3 reflectVector = reflect(-i.worldViewDir.xyz, bumpedNormal.xyz);
			fixed4 refl = texCUBE(_Cube, reflectVector); 
			
			fixed4 selfIllumin = tex;
			
			tex += refl * saturate(tex.g - _OneMinusReflectivity);			
			
			#ifdef LIGHTMAP_ON
				fixed3 lm = ( DecodeLightmap (tex2D(unity_Lightmap, i.uvLM.xy)));
				tex.rgb *= lm;
			#endif	
			
			tex.rgb += selfIllumin.rgb * selfIllumin.aaa * 2;			
			
			return tex;
			
		}		
		
		#pragma vertex vert
		#pragma fragment frag
		#pragma multi_compile LIGHTMAP_OFF LIGHTMAP_ON
		#pragma fragmentoption ARB_precision_hint_fastest 
	
		ENDCG
	}
} 

// mobile versions: only lightmapped and self illuminated

SubShader {
	Tags { "RenderType"="Opaque" "Reflection" = "RenderReflectionOpaque"}
	LOD 200 
	
	Pass {
		CGPROGRAM
		
		float4 unity_LightmapST;
		sampler2D unity_Lightmap;
		float4 _MainTex_ST;		
		
		v2f vert (appdata_full v) 
		{
			v2f o;
			o.pos = mul (UNITY_MATRIX_MVP, v.vertex);
			o.uv.xy = TRANSFORM_TEX(v.texcoord,_MainTex);
			
			#ifdef LIGHTMAP_ON
				o.uvLM = v.texcoord1.xy * unity_LightmapST.xy + unity_LightmapST.zw;
			#endif
			
			return o; 
		}	
		
		fixed4 frag (v2f i) : COLOR0 
		{
			fixed4 tex = tex2D (_MainTex, i.uv);
			
			#ifdef LIGHTMAP_ON
				fixed3 lm = ( DecodeLightmap (tex2D(unity_Lightmap, i.uvLM)));
				tex.rgb *= lm + tex.aaa;
			#else
				tex.rgb += tex.aaa;			
			#endif	
			
			return tex;		
		}		
		
		#pragma vertex vert
		#pragma fragment frag
		#pragma multi_compile LIGHTMAP_OFF LIGHTMAP_ON
		#pragma fragmentoption ARB_precision_hint_fastest 
	
		ENDCG
	}
} 

FallBack "AngryBots/Fallback"
}
