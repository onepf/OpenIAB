// This is just Marmalade stuff

#include "IwDebug.h"
#include "s3e.h"
#include "IwNUI.h"
#include "ExampleUI.h"

const CColour COLOUR_GREY       (0xffC9C0BC);

bool ListBoxSelect(void *pData,CListBox* list, int item)
{
	list->SetAttribute("selected",0); // reset selection
	return true;
}

ExampleUI::ExampleUI()
{
    app = CreateApp();
    CWindowPtr window = CreateWindow();
    app->AddWindow(window);
    CViewPtr view = CreateView("canvas");  
    CViewPtr grayView = CreateView(CAttributes()
                                   .Set("name",        "grayView")
                                   .Set("x1",          0)
                                   .Set("y1",          0)
                                   .Set("width",       "100%")
                                   .Set("height",      "100%")
                                   .Set("backgroundColour", COLOUR_GREY)); 
    view->AddChild(grayView);   
	// buttons
    CButtonPtr button1 = CreateButton(CAttributes()
                                      .Set("name",    "Button1")
                                      .Set("caption", "Query Shop")
                                      .Set("x1", "5%")
                                      .Set("y1", "5%")
                                      .Set("alignW",  "left")
									  .Set("visible", true)
                                      );   
    button1->SetEventHandler("click", (void*)this, &OnButton1Click);   
    CButtonPtr button2 = CreateButton(CAttributes()
                                      .Set("name",    "Button2")
                                      .Set("caption", "Restore Purchases")
                                      .Set("x1", "5%")
                                      .Set("y1", "15%")
                                      .Set("alignW",  "left")
									  .Set("visible", true)
                                      );  
    button2->SetEventHandler("click", (void*)this, &OnButton2Click);
    CButtonPtr button3 = CreateButton(CAttributes()
                                      .Set("name",    "Button3")
                                      .Set("caption", "Purchase Hat")
                                      .Set("x1", "50%")
                                      .Set("y1", "5%")
                                      .Set("alignW",  "left")
									  .Set("visible", true)
                                      );  
    button3->SetEventHandler("click", (void*)this, &OnButton3Click);
    CButtonPtr button4 = CreateButton(CAttributes()
                                      .Set("name",    "Button4")
                                      .Set("caption", "Purchase Coin")
                                      .Set("x1", "50%")
                                      .Set("y1", "15%")
                                      .Set("alignW",  "left")
									  .Set("visible", true)
                                      );  
    button4->SetEventHandler("click", (void*)this, &OnButton4Click);
	CButtonPtr button5 = CreateButton(CAttributes()
                                      .Set("name",    "Button5")
                                      .Set("caption", "Purchase Subscription")
                                      .Set("x1", "50%")
                                      .Set("y1", "25%")
                                      .Set("alignW",  "left")
									  .Set("visible", true)
                                      );  
    button5->SetEventHandler("click", (void*)this, &OnButton5Click);


	// text fields
    statusText = CreateLabel(CAttributes()
                            .Set("caption","")
                            .Set("x1", "5%")
							.Set("x2", "95%")
                            .Set("y1", "60%")
                            .Set("height","10%")
							.Set("alignW","left")
                            );      
	animatingText = CreateLabel(CAttributes()
                            .Set("caption","... Some Animating Text ...")
                            .Set("x1", "5%")
							.Set("x2", "95%")
                            .Set("y1", "70%")
                            .Set("height","10%")
							.Set("alignW","left")
                            ); 
	consumableText = CreateLabel(CAttributes()
                            .Set("caption","")
                            .Set("x1", "5%")
							.Set("x2", "95%")
                            .Set("y1", "50%")
                            .Set("height","10%")
							.Set("alignW","left")
                            ); 

	// log list box
	logText = CreateListBox(CAttributes()
                            .Set("x1", "5%")
							.Set("x2", "95%")
                            .Set("y1", "80%")
							.Set("alignW","left")
                            );   
	logText->SetEventHandler("selectitem",(void*)this,&ListBoxSelect);

	listBoxItems.AddString(CString("View Log"));
	logText->SetAttribute("listBoxItems",listBoxItems);

	view->AddChild(button1); buttons.push_back(button1);
    view->AddChild(button2); buttons.push_back(button2);
    view->AddChild(button3); buttons.push_back(button3);
    view->AddChild(button4); buttons.push_back(button4);
	view->AddChild(button5); buttons.push_back(button5);
 
	view->AddChild(statusText);
	view->AddChild(animatingText);
	view->AddChild(consumableText);
    view->AddChild(logText);
	//view->AddChild(userNameText);

	SetStatusText("");
    window->SetChild(view);   
    app->ShowWindow(window);  
}

void ExampleUI::SetStatusText(const string &msg)
{
	statusText->SetAttribute("caption",msg.c_str());
}

void ExampleUI::SetAnimatingText(const string &msg)
{
	animatingText->SetAttribute("caption",msg.c_str());
}

void ExampleUI::SetConsumableText(const string &msg)
{
	string str = "Consumable product ID : " + msg;
	consumableText->SetAttribute("caption",str.c_str());
}

void ExampleUI::Update()
{
	app->Update();
}

void ExampleUI::Log(const string &msg)
{
	IwTrace(ANDROIDGOOGLEPLAYBILLING_VERBOSE, (msg.c_str()));
	listBoxItems.AddString(CString(msg.c_str()));
	logText->SetAttribute("listBoxItems",listBoxItems);
	SetStatusText(msg);
}

void ExampleUI::EnableAllButtons(bool enable)
{
	for (size_t i=0;i<buttons.size();i++)
		buttons[i]->SetAttribute("visible",enable);
}

// helper function
std::string string_format(const std::string fmt, ...) {
    int size = 100;
    std::string str;
    va_list ap;
    while (1) {
        str.resize(size);
        va_start(ap, fmt);
        int n = vsnprintf((char *)str.c_str(), size, fmt.c_str(), ap);
        va_end(ap);
        if (n > -1 && n < size) {
            str.resize(n);
            return str;
        }
        if (n > -1)
            size = n + 1;
        else
            size *= 2;
    }
    return str;
}