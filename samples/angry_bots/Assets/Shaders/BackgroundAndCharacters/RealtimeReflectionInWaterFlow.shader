

Shader "AngryBots/RealtimeReflectionInWaterFlow" {
	
Properties {
	_MainTex ("Base", 2D) = "white" {}
	_Normal("Normal", 2D) = "bump" {}
	_ReflectionTex("_ReflectionTex", 2D) = "black" {}
	_FakeReflect("Fake reflection", 2D) = "black" {}
	_DirectionUv("Wet scroll direction (2 samples)", Vector) = (1.0,1.0, -0.2,-0.2)
	_TexAtlasTiling("Tex atlas tiling", Vector) = (8.0,8.0, 4.0,4.0)	
}

CGINCLUDE		

struct v2f_full
{
	half4 pos : SV_POSITION;
	half2 uv : TEXCOORD0;
	half4 normalScrollUv : TEXCOORD1;	
	half4 screen : TEXCOORD2;
	half2 fakeRefl : TEXCOORD3;
	#ifdef LIGHTMAP_ON
		half2 uvLM : TEXCOORD4;
	#endif	
};
	
#include "AngryInclude.cginc"		

half4 _DirectionUv;
half4 _TexAtlasTiling;

sampler2D _MainTex;
sampler2D _Normal;		
sampler2D _ReflectionTex;
sampler2D _FakeReflect;
			
ENDCG 

SubShader {
	Tags { "RenderType"="Opaque" }

	LOD 300 

	Pass {
		CGPROGRAM
		
		float4 _MainTex_ST;
		float4 unity_LightmapST;	
		sampler2D unity_Lightmap;
		
		v2f_full vert (appdata_full v) 
		{
			v2f_full o;
			o.pos = mul (UNITY_MATRIX_MVP, v.vertex);
			o.uv.xy = TRANSFORM_TEX(v.texcoord,_MainTex);
			
			#ifdef LIGHTMAP_ON
				o.uvLM = v.texcoord1.xy * unity_LightmapST.xy + unity_LightmapST.zw;
			#endif
			
			o.normalScrollUv.xyzw = v.texcoord.xyxy * _TexAtlasTiling + _Time.xxxx * _DirectionUv;
						
			o.fakeRefl = EthansFakeReflection(v.vertex);
			o.screen = ComputeScreenPos(o.pos);
				
			return o; 
		}
				
		fixed4 frag (v2f_full i) : COLOR0 
		{
			half3 nrml = UnpackNormal(tex2D(_Normal, i.normalScrollUv.xy));
			nrml += UnpackNormal(tex2D(_Normal, i.normalScrollUv.zw));
			
			nrml.xy *= 0.025;
										
			fixed4 rtRefl = tex2D (_ReflectionTex, (i.screen.xy / i.screen.w) + nrml.xy);
			rtRefl += tex2D (_FakeReflect, i.fakeRefl + nrml.xy * 2.0);
						
			fixed4 tex = tex2D (_MainTex, i.uv.xy + nrml.xy * 0.05);
		
			#ifdef LIGHTMAP_ON
				fixed3 lm = ( DecodeLightmap (tex2D(unity_Lightmap, i.uvLM)));
				tex.rgb *= lm;
			#endif	
			
			tex  = tex + tex.a * rtRefl;
			
			return tex;	
		}	
		
		#pragma vertex vert
		#pragma fragment frag
		#pragma multi_compile LIGHTMAP_OFF LIGHTMAP_ON
		#pragma fragmentoption ARB_precision_hint_fastest 
	
		ENDCG
	}
} 


SubShader {
	Tags { "RenderType"="Opaque" }

	LOD 200 

	Pass {
		CGPROGRAM
		
		float4 _MainTex_ST;
		float4 unity_LightmapST;	
		sampler2D unity_Lightmap;
		
		v2f_full vert (appdata_full v) 
		{
			v2f_full o;
			o.pos = mul (UNITY_MATRIX_MVP, v.vertex);
			o.uv = TRANSFORM_TEX(v.texcoord,_MainTex);
			
			#ifdef LIGHTMAP_ON
				o.uvLM = v.texcoord1.xy * unity_LightmapST.xy + unity_LightmapST.zw;
			#endif
			
			o.normalScrollUv.xyzw = v.texcoord.xyxy * _TexAtlasTiling + _Time.xxxx * _DirectionUv;
						
			o.fakeRefl = EthansFakeReflection(v.vertex);
			o.screen = ComputeScreenPos(o.pos);
				
			return o; 
		}
				
		fixed4 frag (v2f_full i) : COLOR0 
		{
			// assuming this is on mobile, so no texture unpacking needed
			
			fixed4 nrml = tex2D(_Normal, i.normalScrollUv.xy);
			nrml = (nrml - 0.5) * 0.1;
										
			fixed4 rtRefl = tex2D (_ReflectionTex, (i.screen.xy / i.screen.w) + nrml.xy);
			
			// needed optimization for now
			//rtRefl += tex2D (_FakeReflect, i.fakeRefl + nrml.xy);
						
			fixed4 tex = tex2D (_MainTex, i.uv);
		
			#ifdef LIGHTMAP_ON
				fixed3 lm = ( DecodeLightmap (tex2D(unity_Lightmap, i.uvLM)));
				tex.rgb *= lm;
			#endif	
			
			tex  = tex + tex.a * rtRefl;
			
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
