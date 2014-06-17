using UnityEngine;
using System.Collections;

public enum AnchorSide
{
    BottomLeft,
    BottomRight,
    TopLeft,
    TopRight
}

public class Anchor : MonoBehaviour
{
    [SerializeField]
    AnchorSide _anchorSide = AnchorSide.BottomLeft;

    [SerializeField]
    Vector2 _offset = Vector2.zero;

    [SerializeField]
    Camera _camera = null;

    void Awake()
    {
        switch (_anchorSide)
        {
            case AnchorSide.BottomLeft:
                transform.position = (Vector2) _camera.ViewportToWorldPoint(new Vector2(0, 0)) + _offset;
                break;
            case AnchorSide.BottomRight:
                transform.position = (Vector2) _camera.ViewportToWorldPoint(new Vector2(1, 0)) + _offset;
                break;
            case AnchorSide.TopLeft:
                transform.position = (Vector2) _camera.ViewportToWorldPoint(new Vector2(0, 1)) + _offset;
                break;
            case AnchorSide.TopRight:
                transform.position = (Vector2) _camera.ViewportToWorldPoint(new Vector2(1, 1)) + _offset;
                break;
        }        
    }
}
