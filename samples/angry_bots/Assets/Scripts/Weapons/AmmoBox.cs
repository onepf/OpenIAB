using UnityEngine;
using System.Collections;

public class AmmoBox : MonoBehaviour {

    private const int MAX_ROUNDS = 10;

    private int _nRounds = 2;
    private bool _isInfinite = false;

    public int Count { get { return _nRounds; } }

    public bool IsFull { get { return _nRounds >= MAX_ROUNDS; } }

    public bool IsInfinite { get { return _isInfinite; } set { _isInfinite = value; SendMessage("SetInfiniteAmmo", value); } }

    [SerializeField]
    AudioSource _reloadAudioSource = null;

    void Awake() {
        _nRounds = PlayerPrefs.GetInt("nRounds", 2);
    }

    void SaveData() {
        PlayerPrefs.SetInt("nRounds", _nRounds);
    }

    public void Supply(int nRounds) {
        _nRounds += nRounds;
        if (_nRounds > MAX_ROUNDS) {
            _nRounds = MAX_ROUNDS;
        }
        SaveData();
    }

    public bool Use() {
        if (_nRounds > 0) {
            --_nRounds;
            _reloadAudioSource.Play();
            SendMessage("Reload");
            SaveData();
            return true;
        } else {
            return false;
        }
    }
}
