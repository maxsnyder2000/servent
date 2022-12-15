<MAIN>
    <TABLE name="Table1">
        <Column1 public long default={() => Date.now() / 1000} label="Date">
            {(l) => new Date(l * 1000).toString()}
        </Column1>
        <Column2 public string condition={(s) => s.length !== 0} label="Text" />
        <Column3 public int default={() => 0} label="Likes" />
        <Column4 void label="Actions">
            <>
                <button onClick={Table1.PATCH.Column3.INC}>
                    Like
                </button>
                <button onClick={Table1.PATCH.Column3.DEC}>
                    Dislike
                </button>
            </>
        </Column4>
    </TABLE>
    <br />
    <input onChange={Table1.POST.Column2.onChange} placeholder="Text" value={Table1.POST.Column2.value} />
    <button onClick={Table1.POST}>
        Post
    </button>
    <button onClick={Table1.DELETE}>
        Delete All
    </button>
    <br />
    <br />
    {Table1.GET.LENGTH > 0 && Table1.GET}
</MAIN>
