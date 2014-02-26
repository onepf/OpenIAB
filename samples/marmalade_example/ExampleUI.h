// This is just Marmalade stuff

#ifndef MARMALADENUI_H
#define MARMALADENUI_H

#define WHISPER_BLOB // comment out to use Multi File WhisperSync - don't mix the two

using namespace std;
using namespace IwNUI;

class ExampleUI
{
public:
	ExampleUI();
	void Update();
	void SetStatusText(const string &msg);
	void SetAnimatingText(const string &msg);
	void SetConsumableText(const string &msg);
	void Log(const string &msg);

	void EnableAllButtons(bool enable);
	std::vector<CButtonPtr> buttons;

private:
	CAppPtr app;
	CViewPtr view;
	CLabelPtr statusText;
	CLabelPtr animatingText;
	CLabelPtr consumableText;
	CListBoxPtr logText;
	CStringArray listBoxItems;
};

// helper function
extern std::string string_format(const std::string fmt, ...);

// NUI takes C style callbacks to handle events 
extern bool OnButton1Click(void* data, CButton* button);
extern bool OnButton2Click(void* data, CButton* button);
extern bool OnButton3Click(void* data, CButton* button);
extern bool OnButton4Click(void* data, CButton* button);
extern bool OnButton5Click(void* data, CButton* button);
extern bool OnButton6Click(void* data, CButton* button);

#endif // defined MARMALADENUI_H